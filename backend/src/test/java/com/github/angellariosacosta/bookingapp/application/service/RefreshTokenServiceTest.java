package com.github.angellariosacosta.bookingapp.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.domain.model.User;

class RefreshTokenServiceTest {

	private static final Long USER_ID = 7L;
	private static final String RAW_TOKEN = "raw";
	private static final String TOKEN_HASH = "hash";
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final String IP = "10.0.0.1";
	private static final Instant SESSION_STARTED = Instant.parse("2026-11-01T10:00:00Z");

	private RefreshTokenRepositoryPort refreshTokens;
	private RefreshTokenGenerator generator;
	private UserRepository userRepository;
	private AuthTokenIssuer issuer;
	private RefreshTokenService service;

	@BeforeEach
	void setUp() {
		refreshTokens = mock(RefreshTokenRepositoryPort.class);
		generator = mock(RefreshTokenGenerator.class);
		userRepository = mock(UserRepository.class);
		issuer = mock(AuthTokenIssuer.class);
		service = new RefreshTokenService(refreshTokens, generator, userRepository, issuer);
	}

	@Test
	void keepsTheOriginalSessionStartWhenRotating() {
		User user = User.builder().id(USER_ID).build();
		RefreshToken stored = RefreshToken.builder().id(1L).userId(USER_ID).tokenHash(TOKEN_HASH)
				.sessionStartedAt(SESSION_STARTED).expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build();
		when(generator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
		when(refreshTokens.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(stored));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(issuer.issue(any(), any(), any(), any())).thenReturn(new AuthTokenIssuer.IssuedTokens(null, 2L));

		service.refresh(RAW_TOKEN, USER_AGENT, IP);

		verify(issuer).issue(user, USER_AGENT, IP, SESSION_STARTED);
	}
}
