package com.neatly.hotel.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.RoomType;

/** Row for Admin Customer Booking list. `roomsCount` is the Figma "Amount" column. */
public record AdminBookingSummaryResponse(
		UUID id,
		String customerName,
		int guests,
		String roomType,
		int roomsCount,
		BedType bedType,
		LocalDate checkIn,
		LocalDate checkOut) {

	public static AdminBookingSummaryResponse from(Booking booking) {
		RoomType roomType = booking.getRoomType();
		return new AdminBookingSummaryResponse(
				booking.getId(),
				booking.getGuestFirstName() + " " + booking.getGuestLastName(),
				booking.getGuests(),
				booking.getRoomNameSnapshot(),
				booking.getRoomsCount(),
				roomType != null ? roomType.getBedType() : null,
				booking.getCheckIn(),
				booking.getCheckOut());
	}
}
