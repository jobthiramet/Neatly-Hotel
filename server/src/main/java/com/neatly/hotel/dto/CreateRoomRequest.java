package com.neatly.hotel.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(
		@NotBlank String name,
		@NotBlank String type,
		@NotNull @DecimalMin("0.0") BigDecimal pricePerNight,
		@NotNull @Min(1) Integer capacity) {
}
