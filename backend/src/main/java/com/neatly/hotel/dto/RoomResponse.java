package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.neatly.hotel.model.Room;

public record RoomResponse(
		UUID id,
		String name,
		String type,
		BigDecimal pricePerNight,
		Integer capacity,
		Boolean active) {

	public static RoomResponse from(Room room) {
		return new RoomResponse(
				room.getId(),
				room.getName(),
				room.getType(),
				room.getPricePerNight(),
				room.getCapacity(),
				room.getActive());
	}
}
