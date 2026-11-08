package com.github.angellariosacosta.bookingapp.application.result;

import java.time.Instant;

/**
 * Sesión viva de un usuario. {@code lastActivityAt} es la emisión del refresh token vigente (se renueva con el
 * access token, ~cada 15 min): sirve como "última actividad" aproximada, no como presencia en línea.
 */
public record UserSessionResult(
		Long id,
		String userAgent,
		String ipAddress,
		Instant startedAt,
		Instant lastActivityAt,
		Instant expiresAt) {
}
