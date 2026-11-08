package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.UserSessionResult;
import com.github.angellariosacosta.bookingapp.domain.exception.UserNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.domain.model.User;

class ListUserSessionsServiceTest {

	private static final Long USER_ID = 7L;
	private static final Instant STARTED = Instant.parse("2026-11-01T10:00:00Z");
	private static final Instant ISSUED = Instant.parse("2026-11-07T09:00:00Z");
	private static final Instant EXPIRES = Instant.parse("2026-12-07T09:00:00Z");

	private UserFinder userFinder;
	private RefreshTokenRepositoryPort refreshTokens;
	private ListUserSessionsService service;

	@BeforeEach
	void setUp() {
		userFinder = mock(UserFinder.class);
		refreshTokens = mock(RefreshTokenRepositoryPort.class);
		service = new ListUserSessionsService(userFinder, refreshTokens);
	}

	@Test
	void mapsActiveTokensToSessions() {
		when(userFinder.getOrThrow(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
		RefreshToken token = RefreshToken.builder().id(3L).userId(USER_ID).userAgent("Mozilla/5.0").ipAddress("10.0.0.1")
				.sessionStartedAt(STARTED).issuedAt(ISSUED).expiresAt(EXPIRES).build();
		when(refreshTokens.findActiveByUserId(eq(USER_ID), any(Instant.class))).thenReturn(List.of(token));

		List<UserSessionResult> sessions = service.listActive(USER_ID);

		assertEquals(List.of(new UserSessionResult(3L, "Mozilla/5.0", "10.0.0.1", STARTED, ISSUED, EXPIRES)), sessions);
	}

	@Test
	void returnsEmptyListWhenThereAreNoActiveSessions() {
		when(userFinder.getOrThrow(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
		when(refreshTokens.findActiveByUserId(eq(USER_ID), any(Instant.class))).thenReturn(List.of());

		assertTrue(service.listActive(USER_ID).isEmpty());
	}

	@Test
	void asksForSessionsActiveAtTheCurrentInstant() {
		when(userFinder.getOrThrow(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
		Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		service.listActive(USER_ID);

		org.mockito.Mockito.verify(refreshTokens).findActiveByUserId(eq(USER_ID),
				org.mockito.ArgumentMatchers.argThat(now -> !now.isBefore(before)));
	}

	@Test
	void failsWhenUserDoesNotExist() {
		when(userFinder.getOrThrow(USER_ID)).thenThrow(new UserNotFoundException("not found"));

		assertThrows(UserNotFoundException.class, () -> service.listActive(USER_ID));

		verifyNoInteractions(refreshTokens);
	}
}
