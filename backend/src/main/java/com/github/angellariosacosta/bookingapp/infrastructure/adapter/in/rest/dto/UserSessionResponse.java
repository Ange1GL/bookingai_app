package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

public record UserSessionResponse(
		Long id,
		String userAgent,
		String ipAddress,
		Instant startedAt,
		Instant lastActivityAt,
		Instant expiresAt) {
}
