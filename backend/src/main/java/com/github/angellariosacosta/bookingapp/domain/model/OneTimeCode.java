package com.github.angellariosacosta.bookingapp.domain.model;

import java.time.Duration;
import java.time.Instant;

import lombok.Getter;

/**
 * Código de un solo uso para crear la contraseña. Se guarda solo su hash. Deja de servir al usarse, al caducar o al
 * agotar los intentos fallidos; emitir uno nuevo para el mismo usuario invalida el anterior.
 */
@Getter
public class OneTimeCode {

	public static final int MAX_FAILED_ATTEMPTS = 5;

	private Long id;
	private Long userId;
	private OneTimeCodePurpose purpose;
	private String codeHash;
	private Instant expiresAt;
	private int failedAttempts;
	private Instant consumedAt;
	private Instant createdAt;

	private OneTimeCode() {
	}

	public static OneTimeCode issue(Long userId, OneTimeCodePurpose purpose, String codeHash, Instant now, Duration ttl) {
		OneTimeCode code = new OneTimeCode();
		code.userId = userId;
		code.purpose = purpose;
		code.codeHash = codeHash;
		code.createdAt = now;
		code.expiresAt = now.plus(ttl);
		return code;
	}

	/** Restaura el estado tal cual viene de la base de datos. */
	public static OneTimeCode reconstitute(Long id, Long userId, OneTimeCodePurpose purpose, String codeHash,
			Instant expiresAt, int failedAttempts, Instant consumedAt, Instant createdAt) {
		OneTimeCode code = new OneTimeCode();
		code.id = id;
		code.userId = userId;
		code.purpose = purpose;
		code.codeHash = codeHash;
		code.expiresAt = expiresAt;
		code.failedAttempts = failedAttempts;
		code.consumedAt = consumedAt;
		code.createdAt = createdAt;
		return code;
	}

	public boolean isUsable(Instant now) {
		return consumedAt == null && now.isBefore(expiresAt) && failedAttempts < MAX_FAILED_ATTEMPTS;
	}

	/** Cuenta un intento fallido; al llegar al máximo el código se invalida y hay que pedir otro. */
	public void registerFailure(Instant now) {
		failedAttempts++;
		if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
			consume(now);
		}
	}

	public void consume(Instant now) {
		if (consumedAt == null) {
			consumedAt = now;
		}
	}
}
