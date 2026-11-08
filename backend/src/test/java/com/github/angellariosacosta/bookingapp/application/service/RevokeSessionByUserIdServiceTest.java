package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.UserNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.User;

class RevokeSessionByUserIdServiceTest {

	private static final Long ACTOR_ID = 1L;
	private static final Long USER_ID = 7L;

	private UserFinder userFinder;
	private RefreshTokenRepositoryPort refreshTokens;
	private UserRepository userRepository;
	private RevokeSessionByUserIdService service;

	@BeforeEach
	void setUp() {
		userFinder = mock(UserFinder.class);
		refreshTokens = mock(RefreshTokenRepositoryPort.class);
		userRepository = mock(UserRepository.class);
		service = new RevokeSessionByUserIdService(userFinder, refreshTokens, userRepository);
	}

	@Test
	void revokesRefreshTokensAndBumpsTokenVersion() {
		when(userFinder.getOrThrow(USER_ID)).thenReturn(User.builder().id(USER_ID).build());

		service.revokeSessionByUserId(ACTOR_ID, USER_ID);

		InOrder order = inOrder(refreshTokens, userRepository);
		order.verify(refreshTokens).revokeAllByUserId(USER_ID);
		order.verify(userRepository).incrementTokenVersion(USER_ID);
	}

	@Test
	void doesNothingWhenUserDoesNotExist() {
		when(userFinder.getOrThrow(USER_ID)).thenThrow(new UserNotFoundException("not found"));

		assertThrows(UserNotFoundException.class, () -> service.revokeSessionByUserId(ACTOR_ID, USER_ID));

		verifyNoInteractions(refreshTokens);
		verify(userRepository, never()).incrementTokenVersion(USER_ID);
	}
}
