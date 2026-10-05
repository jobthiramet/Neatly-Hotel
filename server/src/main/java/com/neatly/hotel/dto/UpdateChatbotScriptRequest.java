package com.neatly.hotel.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UpdateChatbotScriptRequest(
		@NotBlank @Size(max = 4000) String greeting,
		@NotBlank @Size(max = 4000) String autoReply,
		@NotEmpty @Valid List<ChatbotTopicRequest> topics) {
}
