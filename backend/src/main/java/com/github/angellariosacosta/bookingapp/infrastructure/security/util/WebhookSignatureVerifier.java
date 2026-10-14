package com.github.angellariosacosta.bookingapp.infrastructure.security.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppProperties;

import lombok.RequiredArgsConstructor;

/** Valida la cabecera {@code X-Hub-Signature-256} de Meta: HMAC-SHA256 del cuerpo crudo con el App Secret. */
@Component
@RequiredArgsConstructor
public class WebhookSignatureVerifier {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String SIGNATURE_PREFIX = "sha256=";

	private final WhatsAppProperties properties;

	public boolean isValid(byte[] rawBody, String signatureHeader) {
		if (rawBody == null || signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
			return false;
		}
		byte[] expected = (SIGNATURE_PREFIX + sign(rawBody)).getBytes(StandardCharsets.UTF_8);
		byte[] received = signatureHeader.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, received);
	}

	private String sign(byte[] rawBody) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(properties.getAppSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			return HexFormat.of().formatHex(mac.doFinal(rawBody));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("No se pudo calcular el HMAC del webhook", ex);
		}
	}
}
