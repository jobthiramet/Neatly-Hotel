package com.neatly.hotel.service;

import java.util.Optional;

/** Turns one guest message into a reply. The conversation is not stored. */
public interface ChatbotAnswerClient {

	boolean isConfigured();

	/**
	 * Model reply for this message, or empty when the model returned no text.
	 * Throws when the call fails.
	 */
	Optional<String> answer(String message, String facts);
}
