package com.neatly.hotel.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatbotTopicResponse(
		String id,
		String label,
		boolean enabled,
		String format,
		String text,
		String title,
		String actionLabel,
		List<String> roomIds,
		List<ChatbotOptionResponse> options) {
}
