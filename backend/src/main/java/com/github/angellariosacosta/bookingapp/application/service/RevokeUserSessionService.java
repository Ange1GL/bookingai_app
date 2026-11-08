package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.RevokeUserSessionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.SessionNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeUserSessionService implements RevokeUserSessionUseCase {

	private static final String SESSION_NOT_FOUND_MESSAGE = "La sesión no existe o ya fue cerrada";

	private final UserFinder userFinder;
	private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

	@Override
	@Transactional
	public void revokeSession(Long actorId, Long userId, Long sessionId) {
		userFinder.getOrThrow(userId);
		// Se filtra por usuario: una sesión ajena responde igual que una inexistente (no se confirma su existencia).
		RefreshToken session = refreshTokenRepositoryPort.findByIdAndUserId(sessionId, userId)
				.filter(RefreshToken::isActive)
				.orElseThrow(() -> new SessionNotFoundException(SESSION_NOT_FOUND_MESSAGE));
		session.setRevoked(true);
		refreshTokenRepositoryPort.save(session);
		log.warn("Session revoked target={} session={} actor={}", userId, sessionId, actorId);
	}
}
