package com.neatly.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotOptionRequest(
		@NotBlank @Size(max = 120) String label,
		@NotBlank @Size(max = 2000) String detail) {
}
