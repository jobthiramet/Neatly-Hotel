package com.neatly.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHotelInfoRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 5000) String description) {
}
