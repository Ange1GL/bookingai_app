package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.SessionNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.UserNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.domain.model.User;

class RevokeUserSessionServiceTest {

	private static final Long ACTOR_ID = 1L;
	private static final Long USER_ID = 7L;
	private static final Long SESSION_ID = 42L;

	private UserFinder userFinder;
	private RefreshTokenRepositoryPort refreshTokens;
	private RevokeUserSessionService service;

	@BeforeEach
	void setUp() {
		userFinder = mock(UserFinder.class);
		refreshTokens = mock(RefreshTokenRepositoryPort.class);
		service = new RevokeUserSessionService(userFinder, refreshTokens);
		when(userFinder.getOrThrow(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
	}

	private RefreshToken token(boolean revoked, Instant expiresAt) {
		return RefreshToken.builder().id(SESSION_ID).userId(USER_ID).revoked(revoked).expiresAt(expiresAt).build();
	}

	@Test
	void revokesTheRefreshTokenOfTheSession() {
		RefreshToken active = token(false, Instant.now().plus(1, ChronoUnit.DAYS));
		when(refreshTokens.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.of(active));

		service.revokeSession(ACTOR_ID, USER_ID, SESSION_ID);

		assertTrue(active.isRevoked());
		verify(refreshTokens).save(active);
	}

	@Test
	void failsWhenTheSessionDoesNotBelongToTheUserOrDoesNotExist() {
		when(refreshTokens.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.empty());

		assertThrows(SessionNotFoundException.class, () -> service.revokeSession(ACTOR_ID, USER_ID, SESSION_ID));

		verify(refreshTokens, never()).save(any());
	}

	@Test
	void failsWhenTheSessionIsAlreadyRevoked() {
		when(refreshTokens.findByIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(token(true, Instant.now().plus(1, ChronoUnit.DAYS))));

		assertThrows(SessionNotFoundException.class, () -> service.revokeSession(ACTOR_ID, USER_ID, SESSION_ID));

		verify(refreshTokens, never()).save(any());
	}

	@Test
	void failsWhenTheSessionAlreadyExpired() {
		when(refreshTokens.findByIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(token(false, Instant.now().minus(1, ChronoUnit.DAYS))));

		assertThrows(SessionNotFoundException.class, () -> service.revokeSession(ACTOR_ID, USER_ID, SESSION_ID));

		verify(refreshTokens, never()).save(any());
	}

	@Test
	void doesNothingWhenUserDoesNotExist() {
		when(userFinder.getOrThrow(USER_ID)).thenThrow(new UserNotFoundException("not found"));

		assertThrows(UserNotFoundException.class, () -> service.revokeSession(ACTOR_ID, USER_ID, SESSION_ID));

		verifyNoInteractions(refreshTokens);
	}
}
