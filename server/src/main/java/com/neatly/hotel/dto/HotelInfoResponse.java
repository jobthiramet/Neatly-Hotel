package com.neatly.hotel.dto;

import java.time.Instant;
import java.util.UUID;

import com.neatly.hotel.model.HotelInfo;

public record HotelInfoResponse(
		UUID id,
		String name,
		String description,
		String logoUrl,
		Instant updatedAt) {

	public static HotelInfoResponse from(HotelInfo hotelInfo) {
		return new HotelInfoResponse(
				hotelInfo.getId(),
				hotelInfo.getName(),
				hotelInfo.getDescription(),
				hotelInfo.getLogoUrl(),
				hotelInfo.getUpdatedAt());
	}
}
