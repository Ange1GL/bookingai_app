package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.SessionResponse;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

	private static final String ASSISTANT_UNAVAILABLE = "El asistente no está disponible en este momento";

	private final ChatClient chatClient;
	private final ChatClient rawChatClient;

	public AgentController(ChatClient chatClient,
	                       @Qualifier("rawChatClient") ChatClient rawChatClient) {
		this.chatClient = chatClient;
		this.rawChatClient = rawChatClient;
	}

	@PostMapping("/session")
	public ResponseEntity<SessionResponse> createSession() {
		return ResponseEntity.ok(new SessionResponse(UUID.randomUUID().toString()));
	}

	@PostMapping("/chat")
	public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, @CurrentUserId Long userId) {
		try {
			String reply = chatClient.prompt()
					.user(request.message())
					.advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId(userId, request.sessionId())))
					.call()
					.content();
			return ResponseEntity.ok(new ChatResponse(reply));
		} catch (Exception ex) {
			log.error("Error processing chat request: {}", ex.getMessage(), ex);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ASSISTANT_UNAVAILABLE);
		}
	}

	@PostMapping("/raw-chat")
	public ResponseEntity<ChatResponse> rawChat(@Valid @RequestBody ChatRequest request) {
		try {
			String reply = rawChatClient.prompt()
					.user(request.message())
					.call()
					.content();
			return ResponseEntity.ok(new ChatResponse(reply));
		} catch (Exception ex) {
			log.error("Error in raw-chat endpoint: {}", ex.getMessage(), ex);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ASSISTANT_UNAVAILABLE);
		}
	}

	// La memoria se indexa por usuario + sesión: un sessionId ajeno nunca resuelve la conversación de otro usuario.
	private static String conversationId(Long userId, String sessionId) {
		return userId + ":" + sessionId;
	}
}
