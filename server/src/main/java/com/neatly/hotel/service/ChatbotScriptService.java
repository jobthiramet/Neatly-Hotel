package com.neatly.hotel.service;

import com.neatly.hotel.dto.AskChatbotResponse;
import com.neatly.hotel.dto.ChatbotScriptResponse;
import com.neatly.hotel.dto.UpdateChatbotScriptRequest;

public interface ChatbotScriptService {

	ChatbotScriptResponse get();

	ChatbotScriptResponse replace(UpdateChatbotScriptRequest request);

	/** Stored auto-reply. The guest message is not interpreted. */
	AskChatbotResponse ask();
}
