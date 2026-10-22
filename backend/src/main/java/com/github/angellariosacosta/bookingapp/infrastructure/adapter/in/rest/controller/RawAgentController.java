package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.domain.exception.AssistantUnavailableException;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Chat directo al modelo, sin memoria ni herramientas, solo para depurar. Existe únicamente en {@code dev}:
 * no pasa por los créditos de AI, así que en qa/producción sería una vía para saltarse el límite de gasto.
 */
@Slf4j
@Profile("dev")
@RestController
@RequestMapping("/api/v1/agent")
public class RawAgentController {

	private final ChatClient rawChatClient;

	public RawAgentController(@Qualifier("rawChatClient") ChatClient rawChatClient) {
		this.rawChatClient = rawChatClient;
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
			throw new AssistantUnavailableException(ex);
		}
	}
}
