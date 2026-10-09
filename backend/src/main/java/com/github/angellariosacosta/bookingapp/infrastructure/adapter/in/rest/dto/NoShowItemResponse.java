package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

public record NoShowItemResponse(
		Long id,
		Long appointmentId,
		Instant createdAt
) {
}
