package com.neatly.hotel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neatly.hotel.dto.BookingResponse;
import com.neatly.hotel.dto.ChangeBookingDatesRequest;
import com.neatly.hotel.dto.CreateBookingRequest;
import com.neatly.hotel.dto.PageResponse;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.BookingItem;
import com.neatly.hotel.model.BookingItemKind;
import com.neatly.hotel.model.BookingPaymentMethod;
import com.neatly.hotel.model.BookingRoom;
import com.neatly.hotel.model.BookingStatus;
import com.neatly.hotel.model.Payment;
import com.neatly.hotel.model.PaymentKind;
import com.neatly.hotel.model.PaymentProvider;
import com.neatly.hotel.model.PaymentStatus;
import com.neatly.hotel.model.PromotionCode;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.model.StripeWebhookEvent;
import com.neatly.hotel.repository.BookingRepository;
import com.neatly.hotel.repository.PaymentRepository;
import com.neatly.hotel.repository.PromotionCodeRepository;
import com.neatly.hotel.repository.RoomTypeRepository;
import com.neatly.hotel.repository.StripeWebhookEventRepository;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

	/** Bookings in these statuses hold their rooms (pending ones only until the hold expires). */
	static final List<BookingStatus> OCCUPYING = List.of(
			BookingStatus.PENDING_PAYMENT,
			BookingStatus.CONFIRMED,
			BookingStatus.CHECKED_IN);
	private static final List<BookingStatus> HISTORY = List.of(
			BookingStatus.CONFIRMED,
			BookingStatus.CHECKED_IN,
			BookingStatus.CHECKED_OUT,
			BookingStatus.COMPLETED,
			BookingStatus.CANCELLED);
	private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");
	private static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);

	private final BookingRepository bookingRepository;
	private final RoomTypeRepository roomTypeRepository;
	private final PromotionCodeRepository promotionCodeRepository;
	private final PaymentRepository paymentRepository;
	private final StripeWebhookEventRepository stripeEventRepository;
	private final StripeCheckoutGateway stripe;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String clientOrigin;

	public BookingServiceImpl(
			BookingRepository bookingRepository,
			RoomTypeRepository roomTypeRepository,
			PromotionCodeRepository promotionCodeRepository,
			PaymentRepository paymentRepository,
			StripeWebhookEventRepository stripeEventRepository,
			StripeCheckoutGateway stripe,
			@Value("${app.client-origin:http://localhost:5173}") String clientOrigin) {
		this.bookingRepository = bookingRepository;
		this.roomTypeRepository = roomTypeRepository;
		this.promotionCodeRepository = promotionCodeRepository;
		this.paymentRepository = paymentRepository;
		this.stripeEventRepository = stripeEventRepository;
		this.stripe = stripe;
		this.clientOrigin = clientOrigin.replaceAll("/+$", "");
	}

	@Override
	public BookingResponse create(String clerkUserId, CreateBookingRequest request) {
		RoomType roomType = roomTypeRepository.findByIdAndDeletedAtIsNull(request.roomTypeId())
				.orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomTypeId()));
		if (request.guests() > roomType.getCapacity() * request.rooms()) {
			throw new ApiException("Guest count exceeds room capacity", HttpStatus.BAD_REQUEST);
		}
		assertAvailable(roomType, request.checkIn(), request.checkOut(), request.rooms(), null);

		if (request.paymentMethod() == BookingPaymentMethod.STRIPE) {
			expireOpenStripeDrafts(clerkUserId);
		}

		BigDecimal nightly = roomType.getPromotionPrice() != null
				? roomType.getPromotionPrice()
				: roomType.getPricePerNight();
		Booking booking = new Booking();
		booking.setUserId(clerkUserId);
		booking.setCheckIn(request.checkIn());
		booking.setCheckOut(request.checkOut());
		booking.setGuests(request.guests());
		booking.setPaymentMethod(request.paymentMethod());
		booking.setGuestFirstName(request.firstName().trim());
		booking.setGuestLastName(request.lastName().trim());
		booking.setGuestEmail(request.email().trim());
		booking.setGuestPhone(request.phoneNumber().trim());
		booking.setGuestCountry(request.country().trim());
		booking.setGuestDateOfBirth(request.dateOfBirth());
		booking.setAdditionalRequest(blankToNull(request.additionalRequest()));
		booking.setStandardRequests(writePreferences(request.standardRequestCodes()));
		booking.setRoomNameSnapshot(roomType.getName());
		booking.setRoomImageUrl(roomType.getImages().stream()
				.filter(image -> Boolean.TRUE.equals(image.getIsMain()))
				.map(image -> image.getUrl())
				.findFirst()
				.orElse(null));
		for (int i = 0; i < request.rooms(); i++) {
			BookingRoom booked = new BookingRoom();
			booked.setBooking(booking);
			booked.setRoomType(roomType);
			booked.setPricePerNight(nightly);
			booking.getRooms().add(booked);
		}
		price(booking, roomType, request.specialRequestCodes(), request.promotionCode());

		if (request.paymentMethod() == BookingPaymentMethod.CASH) {
			booking.setStatus(BookingStatus.CONFIRMED);
			booking.getPayments().add(cashCharge(booking));
			return toResponse(bookingRepository.save(booking), null);
		}

		booking.setStatus(BookingStatus.PENDING_PAYMENT);
		booking.setHoldExpiresAt(Instant.now().plusSeconds(BookingCatalog.HOLD_SECONDS));
		booking = bookingRepository.saveAndFlush(booking);
		return attachStripeSession(booking);
	}

	@Override
	public BookingResponse retryPayment(String clerkUserId, UUID bookingId) {
		Booking booking = requireMine(clerkUserId, bookingId);
		if (booking.getPaymentMethod() != BookingPaymentMethod.STRIPE) {
			throw new ApiException("This booking is not a card payment", HttpStatus.BAD_REQUEST);
		}
		if (booking.getStatus() == BookingStatus.CONFIRMED) {
			return toResponse(booking, null);
		}
		if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && booking.getStatus() != BookingStatus.EXPIRED) {
			throw new ApiException("This booking cannot be paid", HttpStatus.BAD_REQUEST);
		}
		assertAvailable(
				booking.getRoomType(),
				booking.getCheckIn(),
				booking.getCheckOut(),
				booking.getRoomsCount(),
				booking.getId());
		booking.setStatus(BookingStatus.PENDING_PAYMENT);
		booking.setHoldExpiresAt(Instant.now().plusSeconds(BookingCatalog.HOLD_SECONDS));
		return attachStripeSession(booking);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<BookingResponse> listMine(String clerkUserId, Pageable pageable) {
		PageRequest page = PageRequest.of(
				Math.max(pageable.getPageNumber(), 0),
				Math.clamp(pageable.getPageSize() == 0 ? 10 : pageable.getPageSize(), 1, 50));
		return PageResponse.from(
				bookingRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(clerkUserId, HISTORY, page),
				booking -> toResponse(booking, null));
	}

	@Override
	public BookingResponse findMine(String clerkUserId, UUID bookingId) {
		Booking booking = requireMine(clerkUserId, bookingId);
		if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
			syncStripe(booking);
		}
		String clientSecret = null;
		Payment pending = latestStripeCharge(booking).orElse(null);
		if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
				&& pending != null
				&& pending.getStripeCheckoutSessionId() != null) {
			try {
				clientSecret = stripe.retrieveSession(pending.getStripeCheckoutSessionId()).clientSecret();
			} catch (RuntimeException ignored) {
				clientSecret = null;
			}
		}
		return toResponse(booking, clientSecret);
	}

	@Override
	public BookingResponse cancel(String clerkUserId, UUID bookingId) {
		Booking booking = requireMine(clerkUserId, bookingId);
		if (booking.getStatus() != BookingStatus.CONFIRMED) {
			throw new ApiException("This booking cannot be cancelled", HttpStatus.BAD_REQUEST);
		}
		if (isRefundable(booking)) {
			refundPaidStripeCharge(booking);
		}
		booking.setStatus(BookingStatus.CANCELLED);
		booking.setCancelledAt(Instant.now());
		return toResponse(bookingRepository.save(booking), null);
	}

	@Override
	public BookingResponse changeDates(String clerkUserId, UUID bookingId, ChangeBookingDatesRequest request) {
		Booking booking = requireMine(clerkUserId, bookingId);
		if (!canChangeDates(booking)) {
			throw new ApiException("This booking cannot change dates", HttpStatus.BAD_REQUEST);
		}
		int originalNights = nights(booking.getCheckIn(), booking.getCheckOut());
		int nextNights = nights(request.checkIn(), request.checkOut());
		if (nextNights != originalNights) {
			throw new ApiException(
					"The new stay must be exactly " + originalNights + (originalNights == 1 ? " night" : " nights"),
					HttpStatus.BAD_REQUEST);
		}
		assertAvailable(booking.getRoomType(), request.checkIn(), request.checkOut(), booking.getRoomsCount(), booking.getId());
		booking.setCheckIn(request.checkIn());
		booking.setCheckOut(request.checkOut());
		return toResponse(bookingRepository.save(booking), null);
	}

	@Override
	public void handleStripeEvent(String payload, String signature) {
		StripeCheckoutGateway.WebhookEvent event = stripe.parseEvent(payload, signature);
		if (stripeEventRepository.existsById(event.eventId())) {
			return;
		}
		if ("checkout.session.completed".equals(event.type()) && event.checkoutSessionId() != null) {
			confirmPaidSession(event.checkoutSessionId());
		}
		if ("checkout.session.expired".equals(event.type()) && event.checkoutSessionId() != null) {
			expireSession(event.checkoutSessionId());
		}
		StripeWebhookEvent recorded = new StripeWebhookEvent();
		recorded.setEventId(event.eventId());
		recorded.setType(event.type());
		recorded.setStripeObjectId(event.checkoutSessionId());
		recorded.setProcessedAt(Instant.now());
		stripeEventRepository.save(recorded);
	}

	private BookingResponse attachStripeSession(Booking booking) {
		StripeCheckoutGateway.SessionResult session = stripe.createSession(booking, returnUrl(booking.getId()));
		Payment payment = latestStripeCharge(booking).orElseGet(() -> {
			Payment created = stripeCharge(booking);
			booking.getPayments().add(created);
			return created;
		});
		payment.setStatus(PaymentStatus.PENDING);
		payment.setStripeCheckoutSessionId(session.checkoutSessionId());
		payment.setStripePaymentIntentId(session.paymentIntentId());
		payment.setFailureMessage(null);
		bookingRepository.save(booking);
		return toResponse(booking, session.clientSecret());
	}

	private void confirmPaidSession(String checkoutSessionId) {
		Payment payment = paymentRepository.findByStripeCheckoutSessionId(checkoutSessionId).orElse(null);
		if (payment == null) {
			return;
		}
		StripeCheckoutGateway.SessionView session = stripe.retrieveSession(checkoutSessionId);
		if (!"paid".equalsIgnoreCase(session.paymentStatus())) {
			return;
		}
		Booking booking = payment.getBooking();
		if (booking.getStatus() == BookingStatus.CONFIRMED) {
			return;
		}
		payment.setStatus(PaymentStatus.SUCCEEDED);
		payment.setPaidAt(Instant.now());
		payment.setStripePaymentIntentId(session.paymentIntentId());
		payment.setCardBrand(session.cardBrand());
		payment.setCardLast4(session.cardLast4());
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setHoldExpiresAt(null);
	}

	private void expireSession(String checkoutSessionId) {
		paymentRepository.findByStripeCheckoutSessionId(checkoutSessionId).ifPresent(payment -> {
			Booking booking = payment.getBooking();
			if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
				booking.setStatus(BookingStatus.EXPIRED);
				payment.setStatus(PaymentStatus.CANCELLED);
			}
		});
	}

	private void syncStripe(Booking booking) {
		latestStripeCharge(booking)
				.map(Payment::getStripeCheckoutSessionId)
				.filter(id -> id != null && !id.isBlank())
				.ifPresent(sessionId -> {
					try {
						StripeCheckoutGateway.SessionView session = stripe.retrieveSession(sessionId);
						if ("paid".equalsIgnoreCase(session.paymentStatus())) {
							confirmPaidSession(sessionId);
						}
					} catch (ApiException ignored) {
						// Listing a booking should not fail because Stripe is briefly unreachable.
					}
				});
	}

	private void price(Booking booking, RoomType roomType, List<String> specialCodes, String promotionCode) {
		int nights = (int) (booking.getCheckOut().toEpochDay() - booking.getCheckIn().toEpochDay());
		BigDecimal nightly = roomType.getPromotionPrice() != null ? roomType.getPromotionPrice() : roomType.getPricePerNight();
		BigDecimal roomSubtotal = nightly.multiply(BigDecimal.valueOf((long) nights * booking.getRoomsCount()))
				.setScale(2, RoundingMode.HALF_UP);
		booking.getItems().clear();
		booking.getItems().add(item(
				booking,
				BookingItemKind.ROOM,
				"room",
				roomType.getName() + " Room",
				nights * booking.getRoomsCount(),
				nightly,
				roomSubtotal,
				0));

		BigDecimal extras = BigDecimal.ZERO;
		int sort = 1;
		for (String code : distinct(specialCodes)) {
			BookingCatalog.Addon addon = BookingCatalog.addon(code)
					.orElseThrow(() -> new ApiException("Unknown special request: " + code, HttpStatus.BAD_REQUEST));
			booking.getItems().add(item(booking, BookingItemKind.ADDON, addon.code(), addon.label(), 1, addon.price(), addon.price(), sort++));
			extras = extras.add(addon.price());
		}

		BigDecimal discount = BigDecimal.ZERO;
		if (promotionCode != null && !promotionCode.isBlank()) {
			PromotionCode promo = promotionCodeRepository.findByCodeIgnoreCaseAndActiveTrue(promotionCode.trim())
					.orElse(null);
			if (promo != null) {
				discount = promo.getAmountOff().negate().setScale(2, RoundingMode.HALF_UP);
				booking.setPromotionCode(promo);
				booking.getItems().add(item(
						booking,
						BookingItemKind.DISCOUNT,
						promo.getCode(),
						"Promotion Code",
						1,
						promo.getAmountOff().negate(),
						discount,
						sort));
			}
		}

		BigDecimal total = roomSubtotal.add(extras).add(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
		booking.setRoomSubtotal(roomSubtotal);
		booking.setExtrasTotal(extras.setScale(2, RoundingMode.HALF_UP));
		booking.setDiscountTotal(discount);
		booking.setGrandTotal(total);
		booking.setCurrency("THB");
	}

	private void refundPaidStripeCharge(Booking booking) {
		Payment charge = latestStripeCharge(booking)
				.filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)
				.orElse(null);
		if (charge == null) {
			return;
		}
		String paymentIntentId = charge.getStripePaymentIntentId();
		if (paymentIntentId == null || paymentIntentId.isBlank()) {
			throw new ApiException("Could not refund this payment", HttpStatus.BAD_GATEWAY);
		}
		String refundId = stripe.refund(paymentIntentId, booking.getGrandTotal());
		Payment refund = new Payment();
		refund.setBooking(booking);
		refund.setParentPayment(charge);
		refund.setProvider(PaymentProvider.STRIPE);
		refund.setKind(PaymentKind.REFUND);
		refund.setStatus(PaymentStatus.SUCCEEDED);
		refund.setAmount(booking.getGrandTotal());
		refund.setCurrency(booking.getCurrency());
		refund.setStripePaymentIntentId(paymentIntentId);
		refund.setStripeRefundId(refundId);
		refund.setPaidAt(Instant.now());
		booking.getPayments().add(refund);
	}

	private boolean canChangeDates(Booking booking) {
		return booking.getStatus() == BookingStatus.CONFIRMED
				&& booking.getCreatedAt() != null
				&& Instant.now().isBefore(booking.getCreatedAt().plus(Duration.ofHours(24)));
	}

	private boolean isRefundable(Booking booking) {
		Instant checkInAt = booking.getCheckIn().atTime(CHECK_IN_TIME).atZone(HOTEL_ZONE).toInstant();
		return Instant.now().isBefore(checkInAt.minus(Duration.ofHours(24)));
	}

	private static int nights(LocalDate checkIn, LocalDate checkOut) {
		return (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
	}

	private void assertAvailable(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int rooms, UUID excludeId) {
		// Same inventory as GET /api/rooms/available: bookable room units, not room_types.total_units.
		long bookable = roomTypeRepository.countBookableUnits(roomType.getId(), RoomAvailabilityServiceImpl.BLOCKED_STATUSES);
		long occupied = bookingRepository.occupiedUnits(roomType.getId(), checkIn, checkOut, Instant.now(), excludeId, OCCUPYING);
		if (occupied + rooms > bookable) {
			throw new ApiException("This room type is not available for the selected dates", HttpStatus.CONFLICT);
		}
	}

	private void expireOpenStripeDrafts(String clerkUserId) {
		bookingRepository.findByUserIdAndStatus(clerkUserId, BookingStatus.PENDING_PAYMENT).forEach(draft -> {
			draft.setStatus(BookingStatus.EXPIRED);
			latestStripeCharge(draft).ifPresent(payment -> payment.setStatus(PaymentStatus.CANCELLED));
		});
	}

	private Booking requireMine(String clerkUserId, UUID bookingId) {
		return bookingRepository.findByIdAndUserId(bookingId, clerkUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
	}

	private Payment cashCharge(Booking booking) {
		Payment payment = new Payment();
		payment.setBooking(booking);
		payment.setProvider(PaymentProvider.CASH);
		payment.setKind(PaymentKind.CHARGE);
		payment.setStatus(PaymentStatus.UNPAID);
		payment.setAmount(booking.getGrandTotal());
		payment.setCurrency(booking.getCurrency());
		return payment;
	}

	private Payment stripeCharge(Booking booking) {
		Payment payment = new Payment();
		payment.setBooking(booking);
		payment.setProvider(PaymentProvider.STRIPE);
		payment.setKind(PaymentKind.CHARGE);
		payment.setStatus(PaymentStatus.PENDING);
		payment.setAmount(booking.getGrandTotal());
		payment.setCurrency(booking.getCurrency());
		return payment;
	}

	private BookingItem item(
			Booking booking,
			BookingItemKind kind,
			String code,
			String label,
			int quantity,
			BigDecimal unitPrice,
			BigDecimal amount,
			int sortOrder) {
		BookingItem item = new BookingItem();
		item.setBooking(booking);
		item.setKind(kind);
		item.setCode(code);
		item.setLabel(label);
		item.setQuantity(quantity);
		item.setUnitPrice(unitPrice);
		item.setAmount(amount);
		item.setSortOrder(sortOrder);
		return item;
	}

	private java.util.Optional<Payment> latestStripeCharge(Booking booking) {
		return booking.getPayments().stream()
				.filter(payment -> payment.getProvider() == PaymentProvider.STRIPE && payment.getKind() == PaymentKind.CHARGE)
				.reduce((first, second) -> second);
	}

	private String writePreferences(List<String> codes) {
		List<BookingResponse.NamedRequest> selected = new ArrayList<>();
		for (String code : distinct(codes)) {
			BookingCatalog.Preference preference = BookingCatalog.preference(code)
					.orElseThrow(() -> new ApiException("Unknown standard request: " + code, HttpStatus.BAD_REQUEST));
			selected.add(new BookingResponse.NamedRequest(preference.code(), preference.label()));
		}
		try {
			return objectMapper.writeValueAsString(selected);
		} catch (JsonProcessingException ex) {
			throw new ApiException("Could not save standard requests", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private BookingResponse toResponse(Booking booking, String clientSecret) {
		return BookingResponse.from(booking, clientSecret, objectMapper);
	}

	private String returnUrl(UUID bookingId) {
		return clientOrigin + "/booking/complete?bookingId=" + bookingId + "&session_id={CHECKOUT_SESSION_ID}";
	}

	private static List<String> distinct(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().toList();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
