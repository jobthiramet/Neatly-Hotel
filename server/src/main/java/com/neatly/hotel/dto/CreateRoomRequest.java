package com.neatly.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
		@NotBlank @Size(max = 20) String roomNumber,
		@NotBlank @Size(max = 120) String roomType,
		@NotBlank @Size(max = 50) String bedType,
		@NotBlank @Size(max = 50) String status) {
}
