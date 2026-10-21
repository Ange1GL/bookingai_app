package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreatePasswordCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.VerificationTicketPort;
import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidOneTimeCodeException;
import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCodePurpose;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

class CreatePasswordServiceTest {

	private static final long USER_ID = 4L;
	private static final String CODE = "483920";
	private static final String TICKET = "valid-ticket";
	private static final String NEW_PASSWORD = "Brand-new-pass-1";
	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

	private UserRepository userRepository;
	private OneTimeCodeRepositoryPort codes;
	private PasswordHasher hasher;
	private RefreshTokenRepositoryPort refreshTokens;
	private AccountBlockedRepository accountBlocked;
	private VerificationTicketPort tickets;
	private CreatePasswordService service;
	private User user;
	private OneTimeCode pending;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		codes = mock(OneTimeCodeRepositoryPort.class);
		hasher = mock(PasswordHasher.class);
		refreshTokens = mock(RefreshTokenRepositoryPort.class);
		accountBlocked = mock(AccountBlockedRepository.class);
		tickets = mock(VerificationTicketPort.class);
		TokenService tokenService = mock(TokenService.class);
		RefreshTokenGenerator generator = mock(RefreshTokenGenerator.class);

		when(generator.generateRawToken()).thenReturn("raw-refresh");
		when(generator.hash("raw-refresh")).thenReturn("refresh-hash");
		when(refreshTokens.save(any())).thenAnswer(invocation -> {
			RefreshToken token = invocation.getArgument(0);
			token.setId(77L);
			return token;
		});
		when(tokenService.generateToken(USER_ID, "maria")).thenReturn("new-access");
		when(hasher.hash(NEW_PASSWORD)).thenReturn("new-hash");
		when(hasher.matches(CODE, "code-hash")).thenReturn(true);

		service = new CreatePasswordService(userRepository, codes, hasher, refreshTokens, accountBlocked,
				new AuthTokenIssuer(tokenService, generator, refreshTokens), Clock.fixed(NOW, ZoneOffset.UTC),
				new OneTimeCodeChecker(userRepository, codes, hasher), tickets);

		user = User.builder().id(USER_ID).username("maria").email("m@example.com").active(true)
				.roles(Set.of(new Role(1L, Role.USER_NAME, null, true, true, Set.of()))).build();
		pending = OneTimeCode.issue(USER_ID, OneTimeCodePurpose.ACCOUNT_ACTIVATION, "code-hash", NOW.minusSeconds(60),
				Duration.ofMinutes(30));
		when(userRepository.findByUsername("maria")).thenReturn(Optional.of(user));
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(codes.findPendingByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(pending));
		when(accountBlocked.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(tickets.isValid(TICKET, USER_ID, pending.getId(), NOW)).thenReturn(true);
	}

	private CreatePasswordCommand command(String code, String password) {
		return new CreatePasswordCommand("maria", code, password, TICKET, "agent", "1.2.3.4");
	}

	@Test
	void redeemingTheCodeSetsThePasswordConsumesTheCodeAndOpensASession() {
		AuthTokenResult result = service.createPassword(command(CODE, NEW_PASSWORD));

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(savedUser.capture());
		assertEquals("new-hash", savedUser.getValue().getPassword());
		assertEquals(NOW, savedUser.getValue().getPasswordChangedAt());

		verify(codes).save(pending);
		assertNotNull(pending.getConsumedAt());
		assertFalse(pending.isUsable(NOW));

		verify(refreshTokens).revokeAllByUserId(USER_ID);
		assertEquals("new-access", result.accessToken());
		assertEquals("raw-refresh", result.refreshToken());
	}

	@Test
	void successResetsAPreviousLockout() {
		AccountBlocked blocked = AccountBlocked.create(USER_ID);
		when(accountBlocked.findByUserId(USER_ID)).thenReturn(Optional.of(blocked));

		service.createPassword(command(CODE, NEW_PASSWORD));

		verify(accountBlocked).resetAttempts(blocked);
	}

	@Test
	void aWrongCodeCountsAFailureAndChangesNothing() {
		when(hasher.matches("000000", "code-hash")).thenReturn(false);

		assertThrows(InvalidOneTimeCodeException.class, () -> service.createPassword(command("000000", NEW_PASSWORD)));

		assertEquals(1, pending.getFailedAttempts());
		verify(codes).save(pending);
		verify(userRepository, never()).save(any());
		verify(refreshTokens, never()).revokeAllByUserId(any());
	}

	@Test
	void theFifthWrongCodeBurnsTheCodeEvenIfTheNextOneIsRight() {
		when(hasher.matches("000000", "code-hash")).thenReturn(false);

		for (int attempt = 1; attempt <= OneTimeCode.MAX_FAILED_ATTEMPTS; attempt++) {
			assertThrows(InvalidOneTimeCodeException.class, () -> service.createPassword(command("000000", NEW_PASSWORD)));
		}

		assertFalse(pending.isUsable(NOW));
		assertThrows(InvalidOneTimeCodeException.class, () -> service.createPassword(command(CODE, NEW_PASSWORD)));
		verify(userRepository, never()).save(any());
	}

	@Test
	void failedAttemptsSurviveTheRollbackOfTheException() throws NoSuchMethodException {
		Method method = CreatePasswordService.class.getMethod("createPassword", CreatePasswordCommand.class);

		Transactional transactional = method.getAnnotation(Transactional.class);

		assertTrue(Arrays.asList(transactional.noRollbackFor()).contains(InvalidOneTimeCodeException.class));
	}

	@Test
	void anExpiredCodeIsRejected() {
		OneTimeCode expired = OneTimeCode.issue(USER_ID, OneTimeCodePurpose.ACCOUNT_ACTIVATION, "code-hash",
				NOW.minusSeconds(3600), Duration.ofMinutes(30));
		when(codes.findPendingByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(expired));

		assertThrows(InvalidOneTimeCodeException.class, () -> service.createPassword(command(CODE, NEW_PASSWORD)));

		verify(userRepository, never()).save(any());
	}

	@Test
	void unknownUserAndUserWithoutPendingCodeGetTheSameError() {
		when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
		InvalidOneTimeCodeException unknown = assertThrows(InvalidOneTimeCodeException.class,
				() -> service.createPassword(new CreatePasswordCommand("ghost", CODE, NEW_PASSWORD, TICKET, "a", "i")));

		when(codes.findPendingByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());
		InvalidOneTimeCodeException noCode = assertThrows(InvalidOneTimeCodeException.class,
				() -> service.createPassword(command(CODE, NEW_PASSWORD)));

		assertEquals(unknown.getMessage(), noCode.getMessage());
	}

	@Test
	void aDisabledUserCannotRedeemACode() {
		user.setActive(false);

		assertThrows(InvalidOneTimeCodeException.class, () -> service.createPassword(command(CODE, NEW_PASSWORD)));

		verify(userRepository, never()).save(any());
	}

	@Test
	void aWeakPasswordIsRejectedWithoutBurningAnAttempt() {
		assertThrows(InvalidFieldException.class, () -> service.createPassword(command(CODE, "short1")));
		assertThrows(InvalidFieldException.class, () -> service.createPassword(command(CODE, "onlyletterslong")));

		assertEquals(0, pending.getFailedAttempts());
		verify(codes, never()).findPendingByUserIdForUpdate(any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void aCorrectCodeWithoutAValidVerificationTicketIsRejectedAndKeepsTheCodeUsable() {
		when(tickets.isValid(TICKET, USER_ID, pending.getId(), NOW)).thenReturn(false);

		assertThrows(InvalidOneTimeCodeException.class, () -> service.createPassword(command(CODE, NEW_PASSWORD)));

		assertTrue(pending.isUsable(NOW));
		verify(userRepository, never()).save(any());
		verify(refreshTokens, never()).revokeAllByUserId(any());
	}

	@Test
	void commandToStringHidesTheCodeAndThePassword() {
		String text = command(CODE, NEW_PASSWORD).toString();

		assertFalse(text.contains(CODE));
		assertFalse(text.contains(NEW_PASSWORD));
	}
}
