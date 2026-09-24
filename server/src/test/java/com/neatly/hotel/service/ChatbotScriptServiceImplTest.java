package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.neatly.hotel.dto.ChatbotOptionRequest;
import com.neatly.hotel.dto.ChatbotScriptResponse;
import com.neatly.hotel.dto.ChatbotTopicRequest;
import com.neatly.hotel.dto.UpdateChatbotScriptRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.ChatbotScript;
import com.neatly.hotel.repository.ChatbotScriptRepository;

class ChatbotScriptServiceImplTest {

	private final ChatbotScriptRepository repository = mock(ChatbotScriptRepository.class);
	private final ChatbotScriptServiceImpl service = new ChatbotScriptServiceImpl(repository);
	private ChatbotScript script;

	@BeforeEach
	void setUp() {
		script = new ChatbotScript();
		script.setGreeting("Hello");
		script.setAutoReply("Call us");
		script.setTopics("""
				[{"id":"check-in-out","label":"Check-in","enabled":true,"format":"message","text":"From 2 PM"}]
				""");
		when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(script));
		when(repository.saveAndFlush(any(ChatbotScript.class))).thenAnswer(call -> call.getArgument(0));
	}

	@Test
	void getReadsStoredTopics() {
		ChatbotScriptResponse response = service.get();

		assertEquals("Hello", response.greeting());
		assertEquals("check-in-out", response.topics().get(0).id());
		assertEquals("From 2 PM", response.topics().get(0).text());
	}

	@Test
	void replaceTrimsAndDropsFieldsFromOtherFormats() {
		ChatbotScriptResponse response = service.replace(new UpdateChatbotScriptRequest(
				"  Welcome  ",
				"  Call  ",
				List.of(new ChatbotTopicRequest(
						" room-types ",
						" Room Types ",
						"room-type",
						"ignored",
						"  Title  ",
						"  Book  ",
						List.of(" deluxe "),
						null))));

		assertEquals("Welcome", response.greeting());
		assertEquals("Call", response.autoReply());
		assertEquals("room-types", response.topics().get(0).id());
		assertEquals(List.of("deluxe"), response.topics().get(0).roomIds());
		assertEquals(null, response.topics().get(0).text());
		verify(repository).saveAndFlush(script);
	}

	@Test
	void replaceRejectsDuplicateIds() {
		UpdateChatbotScriptRequest request = new UpdateChatbotScriptRequest(
				"Hi",
				"Bye",
				List.of(message("same"), message("same")));

		ApiException ex = assertThrows(ApiException.class, () -> service.replace(request));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void replaceRejectsMessageWithoutText() {
		UpdateChatbotScriptRequest request = new UpdateChatbotScriptRequest(
				"Hi",
				"Bye",
				List.of(new ChatbotTopicRequest("check-in", "Check-in", "message", "  ", null, null, null, null)));

		ApiException ex = assertThrows(ApiException.class, () -> service.replace(request));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void replaceRejectsOptionMissingDetail() {
		UpdateChatbotScriptRequest request = new UpdateChatbotScriptRequest(
				"Hi",
				"Bye",
				List.of(new ChatbotTopicRequest(
						"pay",
						"Pay",
						"option-with-details",
						null,
						"Methods",
						null,
						null,
						List.of(new ChatbotOptionRequest("Cash", "   ")))));

		ApiException ex = assertThrows(ApiException.class, () -> service.replace(request));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
	}

	private static ChatbotTopicRequest message(String id) {
		return new ChatbotTopicRequest(id, "Label", "message", "Text", null, null, null, null);
	}
}
