package com.neatly.hotel.service;

import com.neatly.hotel.dto.AskChatbotResponse;
import com.neatly.hotel.dto.ChatbotScriptResponse;
import com.neatly.hotel.dto.UpdateChatbotScriptRequest;

public interface ChatbotScriptService {

	ChatbotScriptResponse get();

	ChatbotScriptResponse replace(UpdateChatbotScriptRequest request);

	/**
	 * Reply for one unmatched guest message. Uses the model when it is configured,
	 * otherwise the stored auto-reply. The conversation is not stored.
	 */
	AskChatbotResponse ask(String message);
}
