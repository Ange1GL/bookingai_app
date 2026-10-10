package com.github.angellariosacosta.bookingapp.domain.model;

import java.time.Instant;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

import lombok.Getter;

/** Cliente en lista negra de un tenant. Se crea por bloqueo directo del barbero, con motivo opcional. */
@Getter
public class CustomerBlacklist {

	// Debe coincidir con la longitud de customer_blacklist.reason (varchar(250)) en la BD.
	public static final int MAX_REASON_LENGTH = 250;

	private final Long customerId;
	private final Long userId;
	private final String reason;
	private final Instant createdAt;

	private CustomerBlacklist(Long customerId, Long userId, String reason, Instant createdAt) {
		if (customerId == null) {
			throw new InvalidFieldException("customerId", "must not be null");
		}
		if (userId == null) {
			throw new InvalidFieldException("userId", "must not be null");
		}
		if (reason != null && reason.length() > MAX_REASON_LENGTH) {
			throw new InvalidFieldException("reason", "must not exceed " + MAX_REASON_LENGTH + " characters");
		}
		this.customerId = customerId;
		this.userId = userId;
		this.reason = reason;
		this.createdAt = createdAt;
	}

	public static CustomerBlacklist create(Long customerId, Long userId, String reason, Instant now) {
		return new CustomerBlacklist(customerId, userId, normalizeReason(reason), now);
	}

	// Restaura el estado guardado sin volver a aplicar reglas de creacion.
	public static CustomerBlacklist reconstitute(Long customerId, Long userId, String reason, Instant createdAt) {
		return new CustomerBlacklist(customerId, userId, reason, createdAt);
	}

	private static String normalizeReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return null;
		}
		return reason.strip();
	}
}
