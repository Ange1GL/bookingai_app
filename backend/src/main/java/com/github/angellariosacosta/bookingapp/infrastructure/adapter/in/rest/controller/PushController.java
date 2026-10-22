package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.GetPushPublicKeyUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.SendTestPushUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.SubscribeToPushUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.UnsubscribeFromPushUseCase;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PushPublicKeyResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PushSubscriptionRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PushTestResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PushUnsubscribeRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.PushRestMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Recordatorios push de citas. Ver docs/push-reminders.md. */
@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class PushController {

	private final GetPushPublicKeyUseCase getPushPublicKeyUseCase;
	private final SubscribeToPushUseCase subscribeToPushUseCase;
	private final UnsubscribeFromPushUseCase unsubscribeFromPushUseCase;
	private final SendTestPushUseCase sendTestPushUseCase;
	private final PushRestMapper mapper;

	@GetMapping("/public-key")
	public ResponseEntity<PushPublicKeyResponse> publicKey() {
		return ResponseEntity.ok(new PushPublicKeyResponse(getPushPublicKeyUseCase.getPublicKey()));
	}

	@PostMapping("/subscriptions")
	public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscriptionRequest request,
	                                      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
	                                      @CurrentUserId Long userId) {
		subscribeToPushUseCase.subscribe(mapper.toCommand(request, userId, userAgent));
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@DeleteMapping("/subscriptions")
	public ResponseEntity<Void> unsubscribe(@Valid @RequestBody PushUnsubscribeRequest request, @CurrentUserId Long userId) {
		unsubscribeFromPushUseCase.unsubscribe(mapper.toCommand(request, userId));
		return ResponseEntity.noContent().build();
	}

	// Notificación de prueba a los dispositivos del usuario, para confirmar que el aviso le llega.
	@PostMapping("/test")
	public ResponseEntity<PushTestResponse> test(@CurrentUserId Long userId) {
		return ResponseEntity.ok(new PushTestResponse(sendTestPushUseCase.sendTest(userId)));
	}
}
