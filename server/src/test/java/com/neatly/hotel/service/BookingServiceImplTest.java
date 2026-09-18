package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.neatly.hotel.dto.ChangeBookingDatesRequest;
import com.neatly.hotel.dto.CreateBookingRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.BookingPaymentMethod;
import com.neatly.hotel.model.BookingRoom;
import com.neatly.hotel.model.BookingStatus;
import com.neatly.hotel.model.Payment;
import com.neatly.hotel.model.PaymentKind;
import com.neatly.hotel.model.PaymentProvider;
import com.neatly.hotel.model.PaymentStatus;
import com.neatly.hotel.model.PromotionCode;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.repository.BookingRepository;
import com.neatly.hotel.repository.PaymentRepository;
import com.neatly.hotel.repository.PromotionCodeRepository;
import com.neatly.hotel.repository.RoomTypeRepository;
import com.neatly.hotel.repository.StripeWebhookEventRepository;

class BookingServiceImplTest {

	private final BookingRepository bookingRepository = mock(BookingRepository.class);
	private final RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
	private final PromotionCodeRepository promotionCodeRepository = mock(PromotionCodeRepository.class);
	private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
	private final StripeWebhookEventRepository stripeEventRepository = mock(StripeWebhookEventRepository.class);
	private final StripeCheckoutGateway stripe = mock(StripeCheckoutGateway.class);
	private BookingServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new BookingServiceImpl(
				bookingRepository,
				roomTypeRepository,
				promotionCodeRepository,
				paymentRepository,
				stripeEventRepository,
				stripe,
				"http://localhost:5173");
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void cashBookingConfirmsWithoutStripe() {
		RoomType roomType = roomType(new BigDecimal("2500.00"), new BigDecimal("2500.00"));
		when(roomTypeRepository.findByIdAndDeletedAtIsNull(roomType.getId())).thenReturn(Optional.of(roomType));
		when(bookingRepository.occupiedUnits(eq(roomType.getId()), any(), any(), any(), isNull(), any())).thenReturn(0L);

		var response = service.create("user_abc", request(roomType.getId(), BookingPaymentMethod.CASH, "NEATLYNEW400", List.of("airport-transfer")));

		assertEquals(BookingStatus.CONFIRMED, response.status());
		assertNull(response.clientSecret());
		assertEquals(new BigDecimal("2300.00"), response.grandTotal());
		assertEquals(new BigDecimal("2500.00"), response.roomSubtotal());
		assertEquals(new BigDecimal("200.00"), response.extrasTotal());
		assertEquals(new BigDecimal("-400.00"), response.discountTotal());
		verify(stripe, org.mockito.Mockito.never()).createSession(any(), any());

		ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(saved.capture());
		assertEquals(PaymentStatus.UNPAID, saved.getValue().getPayments().get(0).getStatus());
		assertEquals(1, saved.getValue().getRoomsCount());
	}

	@Test
	void rejectsOverlappingStayWhenInventoryIsFull() {
		RoomType roomType = roomType(new BigDecimal("2500.00"), null);
		when(roomTypeRepository.findByIdAndDeletedAtIsNull(roomType.getId())).thenReturn(Optional.of(roomType));
		when(bookingRepository.occupiedUnits(eq(roomType.getId()), any(), any(), any(), isNull(), any())).thenReturn(4L);

		ApiException exception = assertThrows(
				ApiException.class,
				() -> service.create("user_abc", request(roomType.getId(), BookingPaymentMethod.CASH, null, List.of())));
		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}

	@Test
	void cancelRefundsPaidStripeWhenCheckInIsMoreThan24HoursAway() {
		Booking booking = confirmedBooking(BookingPaymentMethod.STRIPE, Instant.now().minus(Duration.ofHours(2)), bangkokToday().plusDays(4));
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));
		when(stripe.refund("pi_abc", new BigDecimal("2500.00"))).thenReturn("re_abc");

		var response = service.cancel("user_abc", booking.getId());

		assertEquals(BookingStatus.CANCELLED, response.status());
		assertNotNull(response.cancelledAt());
		verify(stripe).refund("pi_abc", new BigDecimal("2500.00"));
		assertEquals(PaymentKind.REFUND, booking.getPayments().get(1).getKind());
		assertEquals("re_abc", booking.getPayments().get(1).getStripeRefundId());
	}

	@Test
	void cancelDoesNotRefundWhenCheckInIsWithin24Hours() {
		Booking booking = confirmedBooking(BookingPaymentMethod.STRIPE, Instant.now().minus(Duration.ofHours(2)), bangkokToday());
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));

		var response = service.cancel("user_abc", booking.getId());

		assertEquals(BookingStatus.CANCELLED, response.status());
		verify(stripe, never()).refund(any(), any());
		assertEquals(1, booking.getPayments().size());
	}

	@Test
	void cancelCashSkipsStripe() {
		Booking booking = confirmedBooking(BookingPaymentMethod.CASH, Instant.now().minus(Duration.ofHours(2)), bangkokToday().plusDays(4));
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));

		var response = service.cancel("user_abc", booking.getId());

		assertEquals(BookingStatus.CANCELLED, response.status());
		verify(stripe, never()).refund(any(), any());
	}

	@Test
	void cancelLeavesBookingConfirmedWhenStripeRefundFails() {
		Booking booking = confirmedBooking(BookingPaymentMethod.STRIPE, Instant.now().minus(Duration.ofHours(2)), bangkokToday().plusDays(4));
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));
		when(stripe.refund(any(), any())).thenThrow(new ApiException("Could not refund this payment", HttpStatus.BAD_GATEWAY));

		ApiException exception = assertThrows(ApiException.class, () -> service.cancel("user_abc", booking.getId()));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
		assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
		assertNull(booking.getCancelledAt());
	}

	@Test
	void cancelRejectsWhenAlreadyCancelled() {
		Booking booking = confirmedBooking(BookingPaymentMethod.CASH, Instant.now(), bangkokToday().plusDays(4));
		booking.setStatus(BookingStatus.CANCELLED);
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));

		ApiException exception = assertThrows(ApiException.class, () -> service.cancel("user_abc", booking.getId()));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
	}

	@Test
	void changeDatesUpdatesStayWithin24HoursOfBooking() {
		Booking booking = confirmedBooking(BookingPaymentMethod.CASH, Instant.now().minus(Duration.ofHours(1)), bangkokToday().plusDays(4));
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));
		when(bookingRepository.occupiedUnits(eq(booking.getRoomType().getId()), any(), any(), any(), eq(booking.getId()), any()))
				.thenReturn(0L);

		LocalDate checkIn = bangkokToday().plusDays(6);
		var response = service.changeDates("user_abc", booking.getId(), new ChangeBookingDatesRequest(checkIn, checkIn.plusDays(1)));

		assertEquals(checkIn, response.checkIn());
		assertEquals(checkIn.plusDays(1), response.checkOut());
		assertEquals(new BigDecimal("2500.00"), response.grandTotal());
	}

	@Test
	void changeDatesRejectsAfter24Hours() {
		Booking booking = confirmedBooking(BookingPaymentMethod.CASH, Instant.now().minus(Duration.ofHours(25)), bangkokToday().plusDays(4));
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));

		ApiException exception = assertThrows(
				ApiException.class,
				() -> service.changeDates(
						"user_abc",
						booking.getId(),
						new ChangeBookingDatesRequest(bangkokToday().plusDays(6), bangkokToday().plusDays(7))));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(bookingRepository, never()).occupiedUnits(any(), any(), any(), any(), any(), any());
	}

	@Test
	void changeDatesRejectsLongerStay() {
		Booking booking = confirmedBooking(BookingPaymentMethod.CASH, Instant.now().minus(Duration.ofHours(1)), bangkokToday().plusDays(4));
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));

		ApiException exception = assertThrows(
				ApiException.class,
				() -> service.changeDates(
						"user_abc",
						booking.getId(),
						new ChangeBookingDatesRequest(bangkokToday().plusDays(6), bangkokToday().plusDays(8))));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
	}

	@Test
	void changeDatesRejectsShorterStay() {
		Booking booking = confirmedBooking(BookingPaymentMethod.CASH, Instant.now().minus(Duration.ofHours(1)), bangkokToday().plusDays(4));
		booking.setCheckOut(booking.getCheckIn().plusDays(2));
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));

		LocalDate checkIn = bangkokToday().plusDays(6);
		ApiException exception = assertThrows(
				ApiException.class,
				() -> service.changeDates("user_abc", booking.getId(), new ChangeBookingDatesRequest(checkIn, checkIn.plusDays(1))));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(bookingRepository, never()).occupiedUnits(any(), any(), any(), any(), any(), any());
	}

	@Test
	void changeDatesRejectsWhenRoomIsUnavailable() {
		Booking booking = confirmedBooking(BookingPaymentMethod.CASH, Instant.now().minus(Duration.ofHours(1)), bangkokToday().plusDays(4));
		when(bookingRepository.findByIdAndUserId(booking.getId(), "user_abc")).thenReturn(Optional.of(booking));
		when(bookingRepository.occupiedUnits(eq(booking.getRoomType().getId()), any(), any(), any(), eq(booking.getId()), any()))
				.thenReturn(4L);

		LocalDate checkIn = bangkokToday().plusDays(6);
		ApiException exception = assertThrows(
				ApiException.class,
				() -> service.changeDates("user_abc", booking.getId(), new ChangeBookingDatesRequest(checkIn, checkIn.plusDays(1))));
		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}

	private CreateBookingRequest request(
			UUID roomId,
			BookingPaymentMethod method,
			String promo,
			List<String> extras) {
		return new CreateBookingRequest(
				roomId,
				LocalDate.now().plusDays(2),
				LocalDate.now().plusDays(3),
				2,
				1,
				"Kate",
				"Cho",
				"kate@example.com",
				"0812345678",
				"Thailand",
				LocalDate.of(1990, 1, 1),
				List.of("high-floor"),
				extras,
				"Late snacks please",
				promo,
				method);
	}

	private RoomType roomType(BigDecimal price, BigDecimal promoPrice) {
		RoomType roomType = new RoomType();
		roomType.setId(UUID.fromString("00000000-0000-0000-0001-000000000001"));
		roomType.setName("Superior Garden View");
		roomType.setPricePerNight(price);
		roomType.setPromotionPrice(promoPrice);
		roomType.setCapacity(2);
		roomType.setTotalUnits(4);
		when(promotionCodeRepository.findByCodeIgnoreCaseAndActiveTrue("NEATLYNEW400"))
				.thenReturn(Optional.of(promo()));
		return roomType;
	}

	private PromotionCode promo() {
		PromotionCode promo = new PromotionCode();
		promo.setCode("NEATLYNEW400");
		promo.setAmountOff(new BigDecimal("400.00"));
		promo.setActive(true);
		return promo;
	}

	private Booking confirmedBooking(BookingPaymentMethod method, Instant createdAt, LocalDate checkIn) {
		RoomType roomType = roomType(new BigDecimal("2500.00"), new BigDecimal("2500.00"));
		Booking booking = new Booking();
		booking.setId(UUID.fromString("00000000-0000-0000-0002-000000000001"));
		booking.setUserId("user_abc");
		booking.setBookingNumber("NTESTBOOKING001");
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setPaymentMethod(method);
		booking.setCreatedAt(createdAt);
		booking.setUpdatedAt(createdAt);
		booking.setCheckIn(checkIn);
		booking.setCheckOut(checkIn.plusDays(1));
		booking.setGuests(2);
		booking.setGuestFirstName("Kate");
		booking.setGuestLastName("Cho");
		booking.setGuestEmail("kate@example.com");
		booking.setGuestPhone("0812345678");
		booking.setGuestCountry("Thailand");
		booking.setGuestDateOfBirth(LocalDate.of(1990, 1, 1));
		booking.setStandardRequests("[]");
		booking.setCurrency("THB");
		booking.setRoomSubtotal(new BigDecimal("2500.00"));
		booking.setExtrasTotal(BigDecimal.ZERO);
		booking.setDiscountTotal(BigDecimal.ZERO);
		booking.setGrandTotal(new BigDecimal("2500.00"));
		booking.setRoomNameSnapshot("Superior Garden View");
		BookingRoom room = new BookingRoom();
		room.setBooking(booking);
		room.setRoomType(roomType);
		room.setPricePerNight(new BigDecimal("2500.00"));
		booking.getRooms().add(room);
		Payment charge = new Payment();
		charge.setBooking(booking);
		charge.setProvider(method == BookingPaymentMethod.STRIPE ? PaymentProvider.STRIPE : PaymentProvider.CASH);
		charge.setKind(PaymentKind.CHARGE);
		charge.setStatus(method == BookingPaymentMethod.STRIPE ? PaymentStatus.SUCCEEDED : PaymentStatus.UNPAID);
		charge.setAmount(booking.getGrandTotal());
		charge.setCurrency("THB");
		if (method == BookingPaymentMethod.STRIPE) {
			charge.setStripePaymentIntentId("pi_abc");
		}
		booking.getPayments().add(charge);
		return booking;
	}

	private static LocalDate bangkokToday() {
		return LocalDate.now(ZoneId.of("Asia/Bangkok"));
	}
}
