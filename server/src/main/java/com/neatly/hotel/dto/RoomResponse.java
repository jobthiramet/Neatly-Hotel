package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.Amenity;
import com.neatly.hotel.model.RoomType;

public record RoomResponse(
		UUID id,
		String name,
		BedType bedType,
		Integer sizeSqm,
		Integer capacity,
		BigDecimal pricePerNight,
		BigDecimal promotionPrice,
		String description,
		List<String> amenities,
		RoomImageResponse mainImage,
		List<RoomImageResponse> gallery,
		Instant createdAt,
		Instant updatedAt) {

	public static RoomResponse from(RoomType room) {
		return new RoomResponse(
				room.getId(),
				room.getName(),
				room.getBedType(),
				room.getSizeSqm(),
				room.getCapacity(),
				room.getPricePerNight(),
				room.getPromotionPrice(),
				room.getDescription(),
				room.getAmenities().stream().map(Amenity::getName).toList(),
				room.getImages().stream().filter(image -> image.getIsMain()).findFirst().map(RoomImageResponse::from).orElse(null),
				room.getImages().stream().filter(image -> !image.getIsMain()).map(RoomImageResponse::from).toList(),
				room.getCreatedAt(),
				room.getUpdatedAt());
	}
}
