package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

/** Solicitud pendiente para el administrador: con {@code userId} se llama a {@code POST /users/{id}/reset-password}. */
public record PasswordRecoveryRequestResponse(
		Long requestId,
		Long userId,
		String username,
		String name,
		String lastName,
		String phone,
		Instant requestedAt
) {
}
