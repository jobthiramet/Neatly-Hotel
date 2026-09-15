package com.neatly.hotel.dto;

import java.util.UUID;

import com.neatly.hotel.model.RoomImage;

public record RoomImageResponse(UUID id, String url) {

	public static RoomImageResponse from(RoomImage image) {
		return new RoomImageResponse(image.getId(), image.getUrl());
	}
}
