package com.github.angellariosacosta.bookingapp.domain.model;

import java.time.Instant;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

import lombok.Getter;

/** Inasistencia de un cliente a una cita reservada. Una cita genera, como mucho, un no-show. */
@Getter
public class NoShow {

	private final Long id;
	private final Long customerId;
	private final Long appointmentId;
	private final Long userId;
	private final Instant createdAt;

	private NoShow(Long id, Long customerId, Long appointmentId, Long userId, Instant createdAt) {
		if (customerId == null) {
			throw new InvalidFieldException("customerId", "must not be null");
		}
		if (appointmentId == null) {
			throw new InvalidFieldException("appointmentId", "must not be null");
		}
		if (userId == null) {
			throw new InvalidFieldException("userId", "must not be null");
		}
		this.id = id;
		this.customerId = customerId;
		this.appointmentId = appointmentId;
		this.userId = userId;
		this.createdAt = createdAt;
	}

	public static NoShow register(Long customerId, Long appointmentId, Long userId, Instant now) {
		return new NoShow(null, customerId, appointmentId, userId, now);
	}

	public static NoShow reconstitute(Long id, Long customerId, Long appointmentId, Long userId, Instant createdAt) {
		return new NoShow(id, customerId, appointmentId, userId, createdAt);
	}
}
