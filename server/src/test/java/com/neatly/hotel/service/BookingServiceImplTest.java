package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.neatly.hotel.dto.CreateBookingRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.BookingPaymentMethod;
import com.neatly.hotel.model.BookingStatus;
import com.neatly.hotel.model.PaymentStatus;
import com.neatly.hotel.model.PromotionCode;
import com.neatly.hotel.model.Room;
import com.neatly.hotel.repository.BookingRepository;
import com.neatly.hotel.repository.PaymentRepository;
import com.neatly.hotel.repository.PromotionCodeRepository;
import com.neatly.hotel.repository.RoomRepository;
import com.neatly.hotel.repository.StripeWebhookEventRepository;

class BookingServiceImplTest {

	private final BookingRepository bookingRepository = mock(BookingRepository.class);
	private final RoomRepository roomRepository = mock(RoomRepository.class);
	private final PromotionCodeRepository promotionCodeRepository = mock(PromotionCodeRepository.class);
	private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
	private final StripeWebhookEventRepository stripeEventRepository = mock(StripeWebhookEventRepository.class);
	private final StripeCheckoutGateway stripe = mock(StripeCheckoutGateway.class);
	private BookingServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new BookingServiceImpl(
				bookingRepository,
				roomRepository,
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
		Room room = room(new BigDecimal("2500.00"), new BigDecimal("2500.00"));
		when(roomRepository.findByIdAndDeletedAtIsNull(room.getId())).thenReturn(Optional.of(room));
		when(bookingRepository.occupiedUnits(eq(room.getId()), any(), any(), any(), isNull(), any())).thenReturn(0L);

		var response = service.create("user_abc", request(room.getId(), BookingPaymentMethod.CASH, "NEATLYNEW400", List.of("airport-transfer")));

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
	}

	@Test
	void rejectsOverlappingStayWhenInventoryIsFull() {
		Room room = room(new BigDecimal("2500.00"), null);
		when(roomRepository.findByIdAndDeletedAtIsNull(room.getId())).thenReturn(Optional.of(room));
		when(bookingRepository.occupiedUnits(eq(room.getId()), any(), any(), any(), isNull(), any())).thenReturn(4L);

		ApiException exception = assertThrows(
				ApiException.class,
				() -> service.create("user_abc", request(room.getId(), BookingPaymentMethod.CASH, null, List.of())));
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

	private Room room(BigDecimal price, BigDecimal promoPrice) {
		Room room = new Room();
		room.setId(UUID.fromString("00000000-0000-0000-0001-000000000001"));
		room.setName("Superior Garden View");
		room.setPricePerNight(price);
		room.setPromotionPrice(promoPrice);
		room.setCapacity(2);
		room.setTotalUnits(4);
		when(promotionCodeRepository.findByCodeIgnoreCaseAndActiveTrue("NEATLYNEW400"))
				.thenReturn(Optional.of(promo()));
		return room;
	}

	private PromotionCode promo() {
		PromotionCode promo = new PromotionCode();
		promo.setCode("NEATLYNEW400");
		promo.setAmountOff(new BigDecimal("400.00"));
		promo.setActive(true);
		return promo;
	}
}
