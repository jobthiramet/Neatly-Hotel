package com.neatly.hotel.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neatly.hotel.dto.ChatbotOptionRequest;
import com.neatly.hotel.dto.ChatbotOptionResponse;
import com.neatly.hotel.dto.ChatbotScriptResponse;
import com.neatly.hotel.dto.ChatbotTopicRequest;
import com.neatly.hotel.dto.ChatbotTopicResponse;
import com.neatly.hotel.dto.UpdateChatbotScriptRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.ChatbotScript;
import com.neatly.hotel.repository.ChatbotScriptRepository;

@Service
@Transactional
public class ChatbotScriptServiceImpl implements ChatbotScriptService {

	private static final TypeReference<List<ChatbotTopicResponse>> TOPICS = new TypeReference<>() {
	};

	private final ChatbotScriptRepository repository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ChatbotScriptServiceImpl(ChatbotScriptRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public ChatbotScriptResponse get() {
		ChatbotScript script = find();
		return new ChatbotScriptResponse(script.getGreeting(), script.getAutoReply(), readTopics(script.getTopics()));
	}

	@Override
	public ChatbotScriptResponse replace(UpdateChatbotScriptRequest request) {
		List<ChatbotTopicResponse> topics = normalize(request.topics());
		ChatbotScript script = find();
		script.setGreeting(request.greeting().trim());
		script.setAutoReply(request.autoReply().trim());
		script.setTopics(writeTopics(topics));
		ChatbotScript saved = repository.saveAndFlush(script);
		return new ChatbotScriptResponse(saved.getGreeting(), saved.getAutoReply(), topics);
	}

	private List<ChatbotTopicResponse> normalize(List<ChatbotTopicRequest> topics) {
		Set<String> seen = new HashSet<>();
		List<ChatbotTopicResponse> normalized = new ArrayList<>();
		for (ChatbotTopicRequest topic : topics) {
			String id = topic.id().trim();
			if (!seen.add(id)) {
				throw new ApiException("Duplicate topic id: " + id, HttpStatus.BAD_REQUEST);
			}
			String label = topic.label().trim();
			String format = topic.format().trim();
			normalized.add(switch (format) {
				case "message" -> messageTopic(id, label, topic);
				case "room-type" -> roomTopic(id, label, topic);
				case "option-with-details" -> optionTopic(id, label, topic);
				default -> throw new ApiException("Unknown reply format: " + format, HttpStatus.BAD_REQUEST);
			});
		}
		return normalized;
	}

	private ChatbotTopicResponse messageTopic(String id, String label, ChatbotTopicRequest topic) {
		if (topic.text() == null || topic.text().isBlank()) {
			throw new ApiException("Topic " + id + " needs a reply message", HttpStatus.BAD_REQUEST);
		}
		return new ChatbotTopicResponse(id, label, true, "message", topic.text().trim(), null, null, null, null);
	}

	private ChatbotTopicResponse roomTopic(String id, String label, ChatbotTopicRequest topic) {
		if (topic.title() == null || topic.title().isBlank()) {
			throw new ApiException("Topic " + id + " needs a reply title", HttpStatus.BAD_REQUEST);
		}
		if (topic.actionLabel() == null || topic.actionLabel().isBlank()) {
			throw new ApiException("Topic " + id + " needs a button name", HttpStatus.BAD_REQUEST);
		}
		List<String> roomIds = new ArrayList<>();
		if (topic.roomIds() != null) {
			for (String roomId : topic.roomIds()) {
				if (roomId == null || roomId.isBlank()) {
					throw new ApiException("Topic " + id + " has a blank room id", HttpStatus.BAD_REQUEST);
				}
				roomIds.add(roomId.trim());
			}
		}
		return new ChatbotTopicResponse(
				id, label, true, "room-type", null, topic.title().trim(), topic.actionLabel().trim(), roomIds, null);
	}

	private ChatbotTopicResponse optionTopic(String id, String label, ChatbotTopicRequest topic) {
		if (topic.title() == null || topic.title().isBlank()) {
			throw new ApiException("Topic " + id + " needs a reply title", HttpStatus.BAD_REQUEST);
		}
		if (topic.options() == null || topic.options().isEmpty()) {
			throw new ApiException("Topic " + id + " needs at least one option", HttpStatus.BAD_REQUEST);
		}
		List<ChatbotOptionResponse> options = new ArrayList<>();
		for (ChatbotOptionRequest option : topic.options()) {
			if (option.label().isBlank() || option.detail().isBlank()) {
				throw new ApiException("Topic " + id + " needs a label and details on every option", HttpStatus.BAD_REQUEST);
			}
			options.add(new ChatbotOptionResponse(option.label().trim(), option.detail().trim()));
		}
		return new ChatbotTopicResponse(
				id, label, true, "option-with-details", null, topic.title().trim(), null, null, options);
	}

	private List<ChatbotTopicResponse> readTopics(String json) {
		try {
			return objectMapper.readValue(json, TOPICS);
		} catch (JsonProcessingException ex) {
			throw new ApiException("Chatbot topics could not be read", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String writeTopics(List<ChatbotTopicResponse> topics) {
		try {
			return objectMapper.writeValueAsString(topics);
		} catch (JsonProcessingException ex) {
			throw new ApiException("Chatbot topics could not be saved", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ChatbotScript find() {
		return repository.findFirstByOrderByCreatedAtAsc()
				.orElseThrow(() -> new ResourceNotFoundException("Chatbot script not found"));
	}
}
