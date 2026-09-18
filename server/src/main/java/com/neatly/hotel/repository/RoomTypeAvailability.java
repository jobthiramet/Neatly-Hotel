package com.neatly.hotel.repository;

import java.util.UUID;

/** Per room type: sellable units and units already booked for the requested stay. */
public record RoomTypeAvailability(UUID roomTypeId, long bookableUnits, long bookedUnits) {

	public long availableUnits() {
		return Math.max(0, bookableUnits - bookedUnits);
	}
}
