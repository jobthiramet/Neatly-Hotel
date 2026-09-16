package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.Room;

/** One row of the admin room list. */
public record RoomSummaryResponse(
		UUID id,
		String name,
		String mainImageUrl,
		BigDecimal pricePerNight,
		BigDecimal promotionPrice,
		Integer capacity,
		BedType bedType,
		Integer sizeSqm) {

	public static RoomSummaryResponse from(Room room) {
		return new RoomSummaryResponse(
				room.getId(),
				room.getName(),
				room.getImages().stream().filter(image -> image.getIsMain()).findFirst().map(image -> image.getUrl()).orElse(null),
				room.getPricePerNight(),
				room.getPromotionPrice(),
				room.getCapacity(),
				room.getBedType(),
				room.getSizeSqm());
	}
}
