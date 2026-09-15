package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.neatly.hotel.model.Room;

public record RoomResponse(
		UUID id,
		String roomNumber,
		String roomType,
		String bedType,
		String status,
		BigDecimal pricePerNight,
		Integer capacity,
		Boolean active) {

	public static RoomResponse from(Room room) {
		return new RoomResponse(
				room.getId(),
				room.getRoomNumber(),
				room.getRoomType(),
				room.getBedType(),
				room.getStatus(),
				room.getPricePerNight(),
				room.getCapacity(),
				room.getActive());
	}
}
