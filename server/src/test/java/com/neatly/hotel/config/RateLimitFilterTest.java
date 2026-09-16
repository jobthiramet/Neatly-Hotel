package com.neatly.hotel.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.rate-limit.max-requests=2")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RateLimitFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void roomListReturns429WithRetryAfterOverTheLimit() throws Exception {
		mockMvc.perform(get("/api/rooms").with(ip("10.0.0.1"))).andExpect(status().isOk());
		mockMvc.perform(get("/api/rooms").with(ip("10.0.0.1"))).andExpect(status().isOk());

		mockMvc.perform(get("/api/rooms").with(ip("10.0.0.1")))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.path").value("/api/rooms"));

		mockMvc.perform(get("/api/rooms").with(ip("10.0.0.2"))).andExpect(status().isOk());
	}

	@Test
	void otherPathsAreNotLimited() throws Exception {
		for (int i = 0; i < 4; i++) {
			mockMvc.perform(get("/api/rooms/00000000-0000-0000-0001-000000000001").with(ip("10.0.0.3")))
					.andExpect(status().isOk());
		}
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor ip(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}
}
