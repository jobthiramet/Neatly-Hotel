package com.neatly.hotel.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotTopicRequest(
		@NotBlank @Size(max = 80) String id,
		@NotBlank @Size(max = 120) String label,
		@NotBlank @Size(max = 40) String format,
		@Size(max = 4000) String text,
		@Size(max = 2000) String title,
		@Size(max = 80) String actionLabel,
		List<@NotBlank @Size(max = 80) String> roomIds,
		@Valid List<ChatbotOptionRequest> options) {
}
