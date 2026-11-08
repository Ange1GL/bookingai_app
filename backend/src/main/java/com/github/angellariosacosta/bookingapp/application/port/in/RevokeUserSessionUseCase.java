package com.github.angellariosacosta.bookingapp.application.port.in;

public interface RevokeUserSessionUseCase {

	/**
	 * Cierra una sola sesión del usuario: su refresh token deja de renovar. El access token ya emitido sigue
	 * válido hasta que venza (security.jwt.expiration); para cortar todo de inmediato está RevokeSessionByUserIdUseCase.
	 */
	void revokeSession(Long actorId, Long userId, Long sessionId);
}
