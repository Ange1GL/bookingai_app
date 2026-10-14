package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.ReceiveWhatsAppEventUseCase;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.WhatsAppWebhookPayload;
import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppProperties;
import com.github.angellariosacosta.bookingapp.infrastructure.security.util.WebhookSignatureVerifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Webhook de WhatsApp Cloud API. Es público (sin JWT ni CSRF): se autentica con el token de verificación (GET)
 * y la firma HMAC del cuerpo (POST). Ver docs/whatsapp-webhook.md.
 */
@Slf4j
@RestController
@RequestMapping(WhatsAppWebhookController.WEBHOOK_PATH)
@RequiredArgsConstructor
public class WhatsAppWebhookController {

	public static final String WEBHOOK_PATH = "/api/v1/webhooks/whatsapp";

	private static final String SUBSCRIBE_MODE = "subscribe";
	private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

	private final WhatsAppProperties properties;
	private final WebhookSignatureVerifier signatureVerifier;
	private final ReceiveWhatsAppEventUseCase receiveWhatsAppEvent;
	private final JsonMapper jsonMapper;

	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> verify(
			@RequestParam(name = "hub.mode", required = false) String mode,
			@RequestParam(name = "hub.verify_token", required = false) String token,
			@RequestParam(name = "hub.challenge", required = false) String challenge) {
		if (SUBSCRIBE_MODE.equals(mode) && challenge != null && isVerifyTokenValid(token)) {
			return ResponseEntity.ok(challenge);
		}
		log.warn("Verificación de webhook de WhatsApp rechazada");
		return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	}

	// El cuerpo se recibe como bytes: el HMAC debe calcularse sobre los bytes exactos que firmó Meta.
	@PostMapping
	public ResponseEntity<Void> receive(
			@RequestBody byte[] rawBody,
			@RequestHeader(name = SIGNATURE_HEADER, required = false) String signature) {
		if (!signatureVerifier.isValid(rawBody, signature)) {
			log.warn("Firma inválida en webhook de WhatsApp");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			WhatsAppWebhookPayload payload = jsonMapper.readValue(rawBody, WhatsAppWebhookPayload.class);
			receiveWhatsAppEvent.receive(payload.toCommand());
		} catch (JacksonException ex) {
			// Firmado pero ilegible: se responde 200 para que Meta no reintente durante días.
			log.warn("Payload de WhatsApp ilegible: {}", ex.getOriginalMessage());
		}
		return ResponseEntity.ok().build();
	}

	private boolean isVerifyTokenValid(String token) {
		return token != null && MessageDigest.isEqual(
				token.getBytes(StandardCharsets.UTF_8),
				properties.getVerifyToken().getBytes(StandardCharsets.UTF_8));
	}
}
