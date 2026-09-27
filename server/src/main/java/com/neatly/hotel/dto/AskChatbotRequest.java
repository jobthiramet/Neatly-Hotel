package com.neatly.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskChatbotRequest(
		@NotBlank @Size(max = 500) String message) {
}
