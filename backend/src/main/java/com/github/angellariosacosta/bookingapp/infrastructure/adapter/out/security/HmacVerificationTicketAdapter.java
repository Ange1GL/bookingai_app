package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.VerificationTicketPort;

/**
 * Ticket sin estado: {@code base64url(userId:codeId:expiraEpoch).base64url(HMAC-SHA256)}. La clave se deriva del
 * secreto del JWT con una etiqueta de dominio, así un ticket jamás es válido como otro tipo de token (ni al revés).
 */
@Component
public class HmacVerificationTicketAdapter implements VerificationTicketPort {

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String KEY_DOMAIN = "otp-verification-ticket-v1";
	private static final String PAYLOAD_SEPARATOR = ":";
	private static final String TOKEN_SEPARATOR_REGEX = "\\.";
	private static final String TOKEN_SEPARATOR = ".";
	private static final int TOKEN_PARTS = 2;
	private static final int PAYLOAD_PARTS = 3;
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private final SecretKey ticketKey;

	public HmacVerificationTicketAdapter(SecretKey jwtSecretKey) {
		this.ticketKey = new SecretKeySpec(hmac(jwtSecretKey, KEY_DOMAIN.getBytes(StandardCharsets.UTF_8)), HMAC_SHA256);
	}

	@Override
	public String issue(Long userId, Long codeId, Instant expiresAt) {
		byte[] payload = String.join(PAYLOAD_SEPARATOR, userId.toString(), codeId.toString(),
				Long.toString(expiresAt.getEpochSecond())).getBytes(StandardCharsets.UTF_8);
		return ENCODER.encodeToString(payload) + TOKEN_SEPARATOR + ENCODER.encodeToString(hmac(ticketKey, payload));
	}

	@Override
	public boolean isValid(String ticket, Long userId, Long codeId, Instant now) {
		if (ticket == null) {
			return false;
		}
		String[] parts = ticket.split(TOKEN_SEPARATOR_REGEX, -1);
		if (parts.length != TOKEN_PARTS) {
			return false;
		}
		try {
			byte[] payload = DECODER.decode(parts[0]);
			byte[] signature = DECODER.decode(parts[1]);
			if (!MessageDigest.isEqual(hmac(ticketKey, payload), signature)) {
				return false;
			}
			String[] fields = new String(payload, StandardCharsets.UTF_8).split(PAYLOAD_SEPARATOR, -1);
			return fields.length == PAYLOAD_PARTS
					&& fields[0].equals(userId.toString())
					&& fields[1].equals(codeId.toString())
					&& now.getEpochSecond() < Long.parseLong(fields[2]);
		} catch (IllegalArgumentException malformed) {
			return false;
		}
	}

	private static byte[] hmac(SecretKey key, byte[] data) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			mac.init(key);
			return mac.doFinal(data);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("HMAC-SHA256 no disponible", e);
		}
	}
}
