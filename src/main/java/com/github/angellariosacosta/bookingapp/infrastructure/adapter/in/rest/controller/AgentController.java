package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.SessionResponse;

import lombok.RequiredArgsConstructor;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

	private final ChatClient chatClient;

	@Autowired
	@Qualifier("rawChatClient")
	private ChatClient rawChatClient;

	@PostMapping("/session")
	public ResponseEntity<SessionResponse> createSession() {
		return ResponseEntity.ok(new SessionResponse(UUID.randomUUID().toString()));
	}

	@PostMapping("/chat")
	public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
		try {
			String reply = chatClient.prompt()
					.user(request.message())
					.advisors(spec -> spec.param(
							ChatMemory.CONVERSATION_ID,
							request.sessionId()))
					.call()
					.content();
			return ResponseEntity.ok(new ChatResponse(reply));
		} catch (Exception ex) {

			log.error("Error processing chat request: {}", ex.getMessage(), ex);
			return ResponseEntity.status(500).body(new ChatResponse("Error processing the request: " + ex.getMessage()));
		}
	}

	@PostMapping("/raw-chat")
	public ResponseEntity<ChatResponse> rawChat(@RequestBody ChatRequest request) {
		try {
			String reply = rawChatClient.prompt()
					.user(request.message())
					.call()
					.content();
			return ResponseEntity.ok(new ChatResponse(reply));
		} catch (Exception ex) {
			log.error("Error in raw-chat endpoint: {}", ex.getMessage(), ex);
			return ResponseEntity.status(500).body(new ChatResponse("Error communicating with AI model: " + ex.getMessage()));
		}
	}
}
