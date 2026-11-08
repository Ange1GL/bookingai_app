package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListUserSessionsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.UserSessionResult;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListUserSessionsService implements ListUserSessionsUseCase {

	private final UserFinder userFinder;
	private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

	@Override
	@Transactional(readOnly = true)
	public List<UserSessionResult> listActive(Long userId) {
		userFinder.getOrThrow(userId);
		return refreshTokenRepositoryPort.findActiveByUserId(userId, Instant.now()).stream()
				.map(this::toResult)
				.toList();
	}

	private UserSessionResult toResult(RefreshToken token) {
		return new UserSessionResult(
				token.getId(), token.getUserAgent(), token.getIpAddress(),
				token.getSessionStartedAt(), token.getIssuedAt(), token.getExpiresAt());
	}
}
