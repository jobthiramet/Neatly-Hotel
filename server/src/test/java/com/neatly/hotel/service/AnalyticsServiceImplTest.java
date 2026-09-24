package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.BookingPaymentMethod;
import com.neatly.hotel.model.BookingStatus;
import com.neatly.hotel.repository.BookingRepository;
import com.neatly.hotel.repository.RoomUnitRepository;

class AnalyticsServiceImplTest {

	private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T05:00:00Z"), BANGKOK);
	private final BookingRepository bookings = mock(BookingRepository.class);
	private final RoomUnitRepository rooms = mock(RoomUnitRepository.class);
	private final AnalyticsServiceImpl service = new AnalyticsServiceImpl(bookings, rooms, CLOCK);

	@BeforeEach
	void defaults() {
		when(bookings.findAnalyticsBookings(any(), any(), any())).thenReturn(List.of());
		when(bookings.findDistinctUserIdsBefore(any(), any())).thenReturn(List.of());
	}

	@Test
	void calculatesSummaryTrendsBreakdownsAndRoomSnapshot() {
		Booking newCash = booking("new", "1000", BookingPaymentMethod.CASH, "2026-09-02T03:00:00Z");
		Booking returningCard = booking("returning", "2500", BookingPaymentMethod.STRIPE, "2026-09-04T03:00:00Z");
		Booking previous = booking("old", "1000", BookingPaymentMethod.CASH, "2026-08-02T03:00:00Z");
		when(bookings.findAnalyticsBookings(any(), any(), any())).thenReturn(
				List.of(newCash, returningCard),
				List.of(previous));
		when(bookings.findDistinctUserIdsBefore(any(), any())).thenReturn(List.of("returning"));
		when(rooms.countBookable(any())).thenReturn(10L);
		when(bookings.countOccupiedRooms(any(), any())).thenReturn(2L);
		when(bookings.countBookedRooms(any(), any(), any())).thenReturn(3L);

		var result = service.get(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 24));

		assertEquals(new BigDecimal("2"), result.summary().totalBookings().value());
		assertEquals(new BigDecimal("100.0"), result.summary().totalBookings().changePercent());
		assertEquals(new BigDecimal("3500"), result.summary().totalSales().value());
		assertEquals(24, result.bookingTrend().size());
		assertEquals(new BigDecimal("1"), result.bookingTrend().get(1).value());
		assertEquals(1, result.guestMix().get(0).count());
		assertEquals(new BigDecimal("50.0"), result.guestMix().get(0).percentage());
		assertEquals(1, result.paymentMethods().get(0).count());
		assertEquals(5, result.roomAvailability().available());
	}

	@Test
	void emptyPeriodReturnsZeroesWithoutUndefinedPercentages() {
		var result = service.get(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));

		assertEquals(BigDecimal.ZERO, result.summary().totalSales().value());
		assertNull(result.summary().totalSales().changePercent());
		assertEquals(BigDecimal.ZERO, result.bookingTrend().getFirst().value());
		assertEquals(BigDecimal.ZERO, result.guestMix().getFirst().percentage());
	}

	@Test
	void validatesDateRange() {
		ApiException reversed = assertThrows(ApiException.class,
				() -> service.get(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1)));
		assertEquals(HttpStatus.BAD_REQUEST, reversed.getStatus());
		assertThrows(ApiException.class,
				() -> service.get(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 25)));
		assertThrows(ApiException.class,
				() -> service.get(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 9, 1)));
	}

	private Booking booking(String userId, String total, BookingPaymentMethod method, String createdAt) {
		Booking booking = new Booking();
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setPaymentMethod(method);
		booking.setGrandTotal(new BigDecimal(total));
		booking.setCreatedAt(Instant.parse(createdAt));
		return booking;
	}
}
