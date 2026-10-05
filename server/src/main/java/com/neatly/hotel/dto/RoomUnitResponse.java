package com.neatly.hotel.dto;

import java.util.UUID;

import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.RoomUnit;

public record RoomUnitResponse(
		UUID id,
		String roomNumber,
		Short floor,
		UUID roomTypeId,
		String roomTypeName,
		BedType bedType,
		String statusCode,
		String statusLabel,
		boolean occupied,
		String displayStatus) {

	public static RoomUnitResponse from(RoomUnit unit, boolean occupied) {
		String code = unit.getRoomStatus().getCode();
		String label = unit.getRoomStatus().getLabel();
		return new RoomUnitResponse(
				unit.getId(),
				unit.getRoomNumber(),
				unit.getFloor(),
				unit.getRoomType().getId(),
				unit.getRoomType().getName(),
				unit.getRoomType().getBedType(),
				code,
				label,
				occupied,
				displayStatus(code, label, occupied));
	}

	/** Badge label: ASSIGN_/OUT_OF_ alone; otherwise Vacant/Occupied + housekeeping label. */
	static String displayStatus(String code, String label, boolean occupied) {
		if (code.startsWith("ASSIGN_") || code.startsWith("OUT_OF_")) {
			return label;
		}
		return (occupied ? "Occupied" : "Vacant") + " " + label;
	}
}
