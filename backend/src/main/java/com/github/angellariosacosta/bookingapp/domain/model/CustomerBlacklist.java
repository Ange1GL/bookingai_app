package com.github.angellariosacosta.bookingapp.domain.model;

import java.time.Instant;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

import lombok.Getter;

@Getter
public class CustomerBlacklist {

	public static final int MAX_REASON_LENGTH = 255;
	private static final String AUTO_REASON = "Auto: %d inasistencias";

	private final Long customerId;
	private final Long userId;
	private final String reason;
	private final BlacklistSource source;
	private final Instant createdAt;

	private CustomerBlacklist(Long customerId, Long userId, String reason, BlacklistSource source, Instant createdAt) {
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
		this.source = source;
		this.createdAt = createdAt;
	}

	public static CustomerBlacklist manual(Long customerId, Long userId, String reason, Instant now) {
		return new CustomerBlacklist(customerId, userId, normalizeReason(reason), BlacklistSource.MANUAL, now);
	}

	public static CustomerBlacklist autoNoShow(Long customerId, Long userId, long activeNoShows, Instant now) {
		return new CustomerBlacklist(
				customerId, userId, AUTO_REASON.formatted(activeNoShows), BlacklistSource.AUTO_NO_SHOW, now);
	}

	// Restaura el estado guardado sin volver a aplicar reglas de creacion.
	public static CustomerBlacklist reconstitute(
			Long customerId, Long userId, String reason, BlacklistSource source, Instant createdAt) {
		return new CustomerBlacklist(customerId, userId, reason, source, createdAt);
	}

	private static String normalizeReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return null;
		}
		return reason.strip();
	}
}
