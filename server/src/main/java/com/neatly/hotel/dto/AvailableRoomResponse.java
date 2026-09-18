package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.RoomType;

/** One room type on the Search Result page. */
public record AvailableRoomResponse(
		UUID id,
		String name,
		String mainImageUrl,
		BigDecimal pricePerNight,
		BigDecimal promotionPrice,
		Integer capacity,
		BedType bedType,
		Integer sizeSqm,
		String description,
		long availableUnits) {

	public static AvailableRoomResponse from(RoomType room, long availableUnits) {
		RoomSummaryResponse summary = RoomSummaryResponse.from(room);
		return new AvailableRoomResponse(
				summary.id(),
				summary.name(),
				summary.mainImageUrl(),
				summary.pricePerNight(),
				summary.promotionPrice(),
				summary.capacity(),
				summary.bedType(),
				summary.sizeSqm(),
				room.getDescription(),
				availableUnits);
	}
}
