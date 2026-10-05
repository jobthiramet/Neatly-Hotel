package com.neatly.hotel.dto;

import java.util.UUID;

import com.neatly.hotel.model.RoomTypeImage;

public record RoomImageResponse(UUID id, String url) {

	public static RoomImageResponse from(RoomTypeImage image) {
		return new RoomImageResponse(image.getId(), image.getUrl());
	}
}
