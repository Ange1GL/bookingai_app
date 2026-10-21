package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.ChangePasswordCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AccessTokenRevocationRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.application.result.DecodedToken;
import com.github.angellariosacosta.bookingapp.domain.exception.AccountBlockedException;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

class ChangePasswordServiceTest {

	private static final long USER_ID = 4L;
	private static final String CURRENT = "Temp-Pass#12";
	private static final String NEW_PASSWORD = "Brand-new-pass-1";
	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

	private UserRepository userRepository;
	private PasswordHasher hasher;
	private AccountBlockedRepository accountBlocked;
	private RefreshTokenRepositoryPort refreshTokens;
	private AccessTokenRevocationRepositoryPort revocations;
	private TokenService tokenService;
	private ChangePasswordService service;
	private User user;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		hasher = mock(PasswordHasher.class);
		accountBlocked = mock(AccountBlockedRepository.class);
		refreshTokens = mock(RefreshTokenRepositoryPort.class);
		revocations = mock(AccessTokenRevocationRepositoryPort.class);
		tokenService = mock(TokenService.class);
		RefreshTokenGenerator generator = mock(RefreshTokenGenerator.class);

		when(generator.generateRawToken()).thenReturn("raw-refresh");
		when(generator.hash("raw-refresh")).thenReturn("refresh-hash");
		when(refreshTokens.save(any())).thenAnswer(invocation -> {
			RefreshToken token = invocation.getArgument(0);
			token.setId(77L);
			return token;
		});
		when(tokenService.generateToken(USER_ID, "maria")).thenReturn("new-access");

		AuthTokenIssuer issuer = new AuthTokenIssuer(tokenService, generator, refreshTokens);
		service = new ChangePasswordService(userRepository, hasher, accountBlocked, new FailedLoginAttempts(accountBlocked),
				refreshTokens, new AccessTokenRevoker(tokenService, revocations), issuer,
				Clock.fixed(NOW, ZoneOffset.UTC));

		user = User.builder().id(USER_ID).username("maria").email("m@example.com").password("old-hash")
				.password("old-hash").active(true)
				.roles(Set.of(new Role(1L, Role.USER_NAME, null, true, true, Set.of()))).build();
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(accountBlocked.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(hasher.matches(CURRENT, "old-hash")).thenReturn(true);
		when(hasher.matches(NEW_PASSWORD, "old-hash")).thenReturn(false);
		when(hasher.hash(NEW_PASSWORD)).thenReturn("new-hash");
	}

	private ChangePasswordCommand command(String current, String next) {
		return new ChangePasswordCommand(USER_ID, current, next, "old-access", "agent", "1.2.3.4");
	}

	@Test
	void changesPasswordClearsFlagAndOpensNewSession() {
		when(tokenService.decode("old-access")).thenReturn(new DecodedToken("maria", "old-jti", NOW.plusSeconds(600)));

		AuthTokenResult result = service.changePassword(command(CURRENT, NEW_PASSWORD));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertEquals("new-hash", saved.getValue().getPassword());
		assertEquals(NOW, saved.getValue().getPasswordChangedAt());

		verify(refreshTokens).revokeAllByUserId(USER_ID);
		verify(revocations).revoke(any());
		assertEquals("new-access", result.accessToken());
		assertEquals("raw-refresh", result.refreshToken());
	}

	@Test
	void wrongCurrentPasswordCountsAFailedAttemptAndChangesNothing() {
		when(hasher.matches("wrong", "old-hash")).thenReturn(false);

		AuthException error = assertThrows(AuthException.class, () -> service.changePassword(command("wrong", NEW_PASSWORD)));

		assertEquals(AuthError.INVALID_CURRENT_PASSWORD, error.getError());
		verify(accountBlocked).findByUserIdForUpdate(USER_ID);
		verify(userRepository, never()).save(any());
		verify(refreshTokens, never()).revokeAllByUserId(anyLong());
	}

	@Test
	void failedAttemptSurvivesTheRollbackOfTheAuthException() throws NoSuchMethodException {
		Method method = ChangePasswordService.class.getMethod("changePassword", ChangePasswordCommand.class);

		Transactional transactional = method.getAnnotation(Transactional.class);

		assertTrue(Arrays.asList(transactional.noRollbackFor()).contains(AuthException.class));
	}

	@Test
	void blockedAccountCannotAttemptAChange() {
		AccountBlocked blocked = AccountBlocked.create(USER_ID);
		blocked.incrementNumberOfAttempts();
		blocked.incrementNumberOfAttempts();
		when(accountBlocked.findByUserId(USER_ID)).thenReturn(Optional.of(blocked));

		assertThrows(AccountBlockedException.class, () -> service.changePassword(command(CURRENT, NEW_PASSWORD)));

		verify(hasher, never()).matches(any(), any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void rejectsWeakNewPassword() {
		assertThrows(InvalidFieldException.class, () -> service.changePassword(command(CURRENT, "short1")));
		assertThrows(InvalidFieldException.class, () -> service.changePassword(command(CURRENT, "onlyletterslong")));

		verify(userRepository, never()).save(any());
		verify(refreshTokens, never()).revokeAllByUserId(anyLong());
	}

	@Test
	void rejectsReusingTheCurrentPassword() {
		when(hasher.matches(NEW_PASSWORD, "old-hash")).thenReturn(true);

		assertThrows(InvalidFieldException.class, () -> service.changePassword(command(CURRENT, NEW_PASSWORD)));

		verify(userRepository, never()).save(any());
	}

	@Test
	void successResetsThePreviousFailedAttempts() {
		AccountBlocked blocked = AccountBlocked.create(USER_ID);
		when(accountBlocked.findByUserId(USER_ID)).thenReturn(Optional.of(blocked));

		service.changePassword(command(CURRENT, NEW_PASSWORD));

		verify(accountBlocked).resetAttempts(blocked);
	}

	@Test
	void aMissingAccessTokenDoesNotBreakTheChange() {
		AuthTokenResult result = service.changePassword(
				new ChangePasswordCommand(USER_ID, CURRENT, NEW_PASSWORD, null, "agent", "1.2.3.4"));

		verify(revocations, never()).revoke(any());
		assertEquals("new-access", result.accessToken());
	}

	@Test
	void commandToStringHidesThePasswords() {
		String text = command(CURRENT, NEW_PASSWORD).toString();

		assertFalse(text.contains(CURRENT));
		assertFalse(text.contains(NEW_PASSWORD));
	}
}
