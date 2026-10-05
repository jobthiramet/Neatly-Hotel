package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.RoomType;

/** Full booking row for Admin Customer Booking detail. */
public record AdminBookingDetailResponse(
		UUID id,
		String customerName,
		int guests,
		String roomType,
		int roomsCount,
		BedType bedType,
		LocalDate checkIn,
		LocalDate checkOut,
		int nights,
		Instant bookingDate,
		String paymentMethodText,
		String currency,
		BigDecimal grandTotal,
		List<BookingResponse.LineItem> items,
		String additionalRequest) {

	public static AdminBookingDetailResponse from(Booking booking, ObjectMapper mapper) {
		BookingResponse full = BookingResponse.from(booking, null, mapper);
		RoomType roomType = booking.getRoomType();
		return new AdminBookingDetailResponse(
				full.id(),
				full.guestFirstName() + " " + full.guestLastName(),
				full.guests(),
				full.roomName(),
				full.roomsCount(),
				roomType != null ? roomType.getBedType() : null,
				full.checkIn(),
				full.checkOut(),
				full.nights(),
				full.createdAt(),
				full.paymentMethodText(),
				full.currency(),
				full.grandTotal(),
				full.items(),
				full.additionalRequest());
	}
}
