package com.neatly.hotel.service;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class GeminiChatbotAnswerClientTest {

	@Test
	void blankKeyDoesNotCallGemini() {
		GeminiChatbotAnswerClient client = new GeminiChatbotAnswerClient("", "gemini-2.5-flash", RestClient.create());

		assertFalse(client.isConfigured());
		assertEquals(Optional.empty(), client.answer("Is the pool open?", "Hotel: Neatly"));
	}

	@Test
	void readsModelTextAndKeepsTheKeyOutOfTheUrl() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andExpect(content().string(containsString("credit card or cash only")))
				.andExpect(content().string(containsString("PromptPay")))
				.andExpect(content().string(containsString("Is the pool open?")))
				.andRespond(withSuccess("""
						{"candidates":[{"content":{"parts":[{"text":"The pool closes at 8 PM"}]}}]}
						""", MediaType.APPLICATION_JSON));

		GeminiChatbotAnswerClient client = new GeminiChatbotAnswerClient("test-key", "gemini-2.5-flash", builder.build());

		assertTrue(client.isConfigured());
		assertEquals(Optional.of("The pool closes at 8 PM"), client.answer("Is the pool open?", "Hotel: Neatly"));
		server.verify();
	}

	@Test
	void httpFailurePropagates() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
		GeminiChatbotAnswerClient client = new GeminiChatbotAnswerClient("test-key", "gemini-2.5-flash", builder.build());

		assertThrows(RestClientException.class, () -> client.answer("Is the pool open?", "Hotel: Neatly"));
	}

	@Test
	void emptyCandidateBecomesEmptyReply() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
				.andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));
		GeminiChatbotAnswerClient client = new GeminiChatbotAnswerClient("test-key", "gemini-2.5-flash", builder.build());

		assertEquals(Optional.empty(), client.answer("Is the pool open?", "Hotel: Neatly"));
	}
}
