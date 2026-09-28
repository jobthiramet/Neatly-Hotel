package com.neatly.hotel.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
		"app.chatbot.ask-rate-limit.max-requests=2",
		"app.chatbot.gemini.api-key="
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ChatbotAskRateLimitFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void askReturns429WithRetryAfterOverTheLimit() throws Exception {
		String body = "{\"message\":\"Is the pool open late?\"}";
		mockMvc.perform(post("/api/chatbot/ask").contentType(MediaType.APPLICATION_JSON).content(body).with(ip("10.1.0.1")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/chatbot/ask").contentType(MediaType.APPLICATION_JSON).content(body).with(ip("10.1.0.1")))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/chatbot/ask").contentType(MediaType.APPLICATION_JSON).content(body).with(ip("10.1.0.1")))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.path").value("/api/chatbot/ask"));

		mockMvc.perform(post("/api/chatbot/ask").contentType(MediaType.APPLICATION_JSON).content(body).with(ip("10.1.0.2")))
				.andExpect(status().isOk());
	}

	@Test
	void readingTheScriptIsNotLimited() throws Exception {
		for (int i = 0; i < 4; i++) {
			mockMvc.perform(get("/api/chatbot").with(ip("10.1.0.3"))).andExpect(status().isOk());
		}
	}

	private static RequestPostProcessor ip(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}
}
