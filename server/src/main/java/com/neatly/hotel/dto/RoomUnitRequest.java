package com.neatly.hotel.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoomUnitRequest(
		@NotBlank
		@Pattern(regexp = "^[0-9]{4}$", message = "must be exactly 4 digits")
		String roomNumber,
		@NotNull UUID roomTypeId,
		@NotBlank @Size(max = 50) String statusCode) {
}
