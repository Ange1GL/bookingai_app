package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.SessionResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

	private final ChatClient chatClient;

	@PostMapping("/session")
	public ResponseEntity<SessionResponse> createSession() {
		return ResponseEntity.ok(new SessionResponse(UUID.randomUUID().toString()));
	}

	@PostMapping("/chat")
	public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
		String reply = chatClient.prompt()
				.user(request.message())
				.advisors(spec -> spec.param(
						ChatMemory.CONVERSATION_ID,
						request.sessionId()))
				.call()
				.content();
		return ResponseEntity.ok(new ChatResponse(reply));
	}
}
