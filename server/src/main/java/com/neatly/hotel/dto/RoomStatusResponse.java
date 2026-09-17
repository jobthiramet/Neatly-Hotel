package com.neatly.hotel.dto;

import java.util.UUID;

import com.neatly.hotel.model.RoomStatus;

public record RoomStatusResponse(
		UUID id,
		String code,
		String label,
		Integer sortOrder) {

	public static RoomStatusResponse from(RoomStatus status) {
		return new RoomStatusResponse(status.getId(), status.getCode(), status.getLabel(), status.getSortOrder());
	}
}
