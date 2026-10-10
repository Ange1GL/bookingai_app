package com.github.angellariosacosta.bookingapp.domain.model;

import java.time.Instant;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

import lombok.Getter;

/** Inasistencia de un cliente a una cita reservada. Una cita genera, como mucho, un no-show. */
@Getter
public class NoShow {

	// Debe coincidir con la longitud de customer_no_show.reason (varchar(250)) en la BD.
	public static final int MAX_REASON_LENGTH = 250;

	private final Long id;
	private final Long customerId;
	private final Long appointmentId;
	private final Long userId;
	private final String reason;
	private final Instant createdAt;

	private NoShow(Long id, Long customerId, Long appointmentId, Long userId, String reason, Instant createdAt) {
		if (customerId == null) {
			throw new InvalidFieldException("customerId", "must not be null");
		}
		if (appointmentId == null) {
			throw new InvalidFieldException("appointmentId", "must not be null");
		}
		if (userId == null) {
			throw new InvalidFieldException("userId", "must not be null");
		}
		if (reason != null && reason.length() > MAX_REASON_LENGTH) {
			throw new InvalidFieldException("reason", "must not exceed " + MAX_REASON_LENGTH + " characters");
		}
		this.id = id;
		this.customerId = customerId;
		this.appointmentId = appointmentId;
		this.userId = userId;
		this.reason = reason;
		this.createdAt = createdAt;
	}

	public static NoShow register(Long customerId, Long appointmentId, Long userId, String reason, Instant now) {
		return new NoShow(null, customerId, appointmentId, userId, normalizeReason(reason), now);
	}

	public static NoShow reconstitute(
			Long id, Long customerId, Long appointmentId, Long userId, String reason, Instant createdAt) {
		return new NoShow(id, customerId, appointmentId, userId, reason, createdAt);
	}

	private static String normalizeReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return null;
		}
		return reason.strip();
	}
}
