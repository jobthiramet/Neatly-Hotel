package com.neatly.hotel.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatbotScriptResponse(
		String greeting,
		String autoReply,
		List<ChatbotTopicResponse> topics) {
}
