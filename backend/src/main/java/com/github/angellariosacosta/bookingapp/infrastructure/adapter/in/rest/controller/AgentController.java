package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.angellariosacosta.bookingapp.application.port.in.ChatWithAssistantUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.GetAiCreditsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.GetVoiceQuotaUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.TranscribeVoiceUseCase;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AssistantChatResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreditsResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.SessionResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.TranscriptionResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.VoiceQuotaResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.AssistantRestMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

	private final ChatWithAssistantUseCase chatWithAssistantUseCase;
	private final GetAiCreditsUseCase getAiCreditsUseCase;
	private final TranscribeVoiceUseCase transcribeVoiceUseCase;
	private final GetVoiceQuotaUseCase getVoiceQuotaUseCase;
	private final AssistantRestMapper assistantMapper;

	@PostMapping("/session")
	public ResponseEntity<SessionResponse> createSession() {
		return ResponseEntity.ok(new SessionResponse(UUID.randomUUID().toString()));
	}

	@PostMapping("/chat")
	public ResponseEntity<AssistantChatResponse> chat(@Valid @RequestBody ChatRequest request, @CurrentUserId Long userId) {
		return ResponseEntity.ok(assistantMapper.toResponse(
				chatWithAssistantUseCase.chat(assistantMapper.toCommand(request, userId))));
	}

	// Saldo de créditos de AI del día; el frontend lo consulta al abrir el asistente y tras un 429.
	@GetMapping("/credits")
	public ResponseEntity<CreditsResponse> credits(@CurrentUserId Long userId) {
		return ResponseEntity.ok(assistantMapper.toResponse(getAiCreditsUseCase.getCredits(userId)));
	}

	// Dictado por voz: devuelve el texto para que el usuario lo revise en la caja del chat antes de enviarlo.
	@PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<TranscriptionResponse> transcribe(@RequestPart("audio") MultipartFile audio,
	                                                        @CurrentUserId Long userId) {
		return ResponseEntity.ok(assistantMapper.toResponse(
				transcribeVoiceUseCase.transcribe(assistantMapper.toCommand(audio, userId))));
	}

	// Dictados usados hoy y hora de recarga, para el medidor del chat.
	@GetMapping("/transcriptions/quota")
	public ResponseEntity<VoiceQuotaResponse> voiceQuota(@CurrentUserId Long userId) {
		return ResponseEntity.ok(assistantMapper.toResponse(getVoiceQuotaUseCase.getQuota(userId)));
	}
}
