package com.neatly.hotel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.ChatbotScriptResponse;
import com.neatly.hotel.dto.UpdateChatbotScriptRequest;
import com.neatly.hotel.service.ChatbotScriptService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Chatbot")
public class ChatbotScriptController {

	private final ChatbotScriptService chatbotScriptService;

	public ChatbotScriptController(ChatbotScriptService chatbotScriptService) {
		this.chatbotScriptService = chatbotScriptService;
	}

	@GetMapping
	@Operation(summary = "Get the guest chatbot script", description = "Public. Greeting, auto-reply and suggestion topics.")
	public ApiResponse<ChatbotScriptResponse> get() {
		return ApiResponse.ok(chatbotScriptService.get());
	}

	@PutMapping
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@Operation(summary = "Replace the guest chatbot script", description = "Requires a Clerk session and agent role in the database profile.")
	public ApiResponse<ChatbotScriptResponse> replace(@Valid @RequestBody UpdateChatbotScriptRequest request) {
		return ApiResponse.ok("Chatbot script updated", chatbotScriptService.replace(request));
	}
}
