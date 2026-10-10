package com.github.angellariosacosta.bookingapp.domain.model;

import java.time.Instant;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

import lombok.Getter;

/** Cliente en lista negra de un tenant. Solo se crea al alcanzar el umbral de inasistencias. */
@Getter
public class CustomerBlacklist {

	private static final String NO_SHOWS_REASON = "Auto: %d inasistencias";

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
		this.customerId = customerId;
		this.userId = userId;
		this.reason = reason;
		this.createdAt = createdAt;
	}

	public static CustomerBlacklist forNoShows(Long customerId, Long userId, long activeNoShows, Instant now) {
		return new CustomerBlacklist(customerId, userId, NO_SHOWS_REASON.formatted(activeNoShows), now);
	}

	// Restaura el estado guardado sin volver a aplicar reglas de creacion.
	public static CustomerBlacklist reconstitute(Long customerId, Long userId, String reason, Instant createdAt) {
		return new CustomerBlacklist(customerId, userId, reason, createdAt);
	}
}
