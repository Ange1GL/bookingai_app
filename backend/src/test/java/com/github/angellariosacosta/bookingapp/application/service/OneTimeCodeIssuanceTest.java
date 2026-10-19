package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeNotifierPort;
import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePhoneException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.SelfModificationException;
import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCodePurpose;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

class OneTimeCodeIssuanceTest {

	private static final String CODE = "483920";
	private static final long ACTOR = 1L;
	private static final long TARGET = 9L;
	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

	private final Role userRole = new Role(11L, Role.USER_NAME, null, true, true, Set.of());
	private final Role disabledRole = new Role(12L, "OLD", null, false, false, Set.of());

	private UserRepository userRepository;
	private RoleRepository roleRepository;
	private PasswordHasher hasher;
	private OneTimeCodeRepositoryPort codes;
	private RefreshTokenRepositoryPort tokens;
	private AccountBlockedRepository accountBlocked;
	private OneTimeCodeNotifierPort notifier;
	private CreateUserService createService;
	private ResetUserPasswordService resetService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		roleRepository = mock(RoleRepository.class);
		hasher = mock(PasswordHasher.class);
		codes = mock(OneTimeCodeRepositoryPort.class);
		tokens = mock(RefreshTokenRepositoryPort.class);
		accountBlocked = mock(AccountBlockedRepository.class);
		notifier = mock(OneTimeCodeNotifierPort.class);
		OneTimeCodeGenerator generator = () -> CODE;
		when(hasher.hash(CODE)).thenReturn("code-hash");
		when(userRepository.save(any())).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			if (user.getId() == null) {
				user.setId(TARGET);
			}
			return user;
		});

		UserFinder finder = new UserFinder(userRepository);
		OneTimeCodeIssuer issuer = new OneTimeCodeIssuer(userRepository, finder, new UserAdminGuard(userRepository),
				new AssignableRoles(roleRepository), generator, hasher, codes, tokens, accountBlocked,
				Clock.fixed(NOW, ZoneOffset.UTC), 30);
		OneTimeCodeDelivery delivery = new OneTimeCodeDelivery(notifier);
		createService = new CreateUserService(issuer, delivery);
		resetService = new ResetUserPasswordService(issuer, delivery);
	}

	private CreateUserCommand command() {
		return new CreateUserCommand(" new.user ", " new@example.com ", " New ", "+52 155 1234 5678", Set.of(11L));
	}

	@Test
	void createsUserWithoutPasswordIssuesAHashedActivationCodeAndSendsItAfterSaving() {
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));
		when(notifier.sendCode(anyString(), anyString())).thenReturn(true);

		IssuedCodeResult result = createService.create(command());

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<OneTimeCode> savedCode = ArgumentCaptor.forClass(OneTimeCode.class);
		InOrder order = inOrder(userRepository, codes, notifier);
		order.verify(userRepository).save(savedUser.capture());
		order.verify(codes).consumePending(TARGET, NOW);
		order.verify(codes).save(savedCode.capture());
		order.verify(notifier).sendCode("5215512345678", CODE);

		User user = savedUser.getValue();
		assertEquals("new.user", user.getUsername());
		assertEquals("new@example.com", user.getEmail());
		assertEquals("New", user.getName());
		assertNull(user.getPassword());
		assertTrue(user.isActive());
		assertEquals(Set.of(userRole), user.getRoles());

		OneTimeCode code = savedCode.getValue();
		assertEquals(TARGET, code.getUserId());
		assertEquals(OneTimeCodePurpose.ACCOUNT_ACTIVATION, code.getPurpose());
		assertEquals("code-hash", code.getCodeHash());
		assertEquals(NOW.plusSeconds(30 * 60), code.getExpiresAt());
		assertTrue(result.delivered());
		assertEquals(CODE, result.code());
	}

	@Test
	void userIsStillCreatedWhenWhatsAppFails() {
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));
		when(notifier.sendCode(anyString(), anyString())).thenReturn(false);

		IssuedCodeResult result = createService.create(command());

		assertFalse(result.delivered());
		assertEquals(TARGET, result.user().getId());
	}

	@Test
	void resultToStringNeverContainsTheCode() {
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));

		assertFalse(createService.create(command()).toString().contains(CODE));
	}

	@Test
	void rejectsDuplicateUsernameEmailAndPhoneWithoutIssuingAnything() {
		when(userRepository.existsByUsername("new.user")).thenReturn(true);
		assertEquals(AuthError.USERNAME_ALREADY_IN_USE,
				assertThrows(AuthException.class, () -> createService.create(command())).getError());

		when(userRepository.existsByUsername("new.user")).thenReturn(false);
		when(userRepository.existsByEmail("new@example.com")).thenReturn(true);
		assertEquals(AuthError.EMAIL_ALREADY_IN_USE,
				assertThrows(AuthException.class, () -> createService.create(command())).getError());

		when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
		when(userRepository.existsByPhone("5215512345678")).thenReturn(true);
		assertThrows(DuplicatePhoneException.class, () -> createService.create(command()));

		verify(userRepository, never()).save(any());
		verify(codes, never()).save(any());
		verify(notifier, never()).sendCode(any(), any());
	}

	@Test
	void rejectsInvalidInputsAndDisabledRolesWithoutIssuingAnything() {
		assertThrows(InvalidFieldException.class, () -> createService.create(
				new CreateUserCommand("ab", "a@b.co", null, "5215512345678", Set.of(11L))));
		assertThrows(InvalidFieldException.class, () -> createService.create(
				new CreateUserCommand("valid", "a@b.co", null, "12", Set.of(11L))));
		assertThrows(InvalidFieldException.class, () -> createService.create(
				new CreateUserCommand("valid", " ", null, "5215512345678", Set.of(11L))));
		assertThrows(InvalidFieldException.class, () -> createService.create(
				new CreateUserCommand("valid", "a@b.co", null, "5215512345678", Set.of())));

		when(roleRepository.findAllByIds(Set.of(12L))).thenReturn(List.of(disabledRole));
		assertThrows(InvalidFieldException.class, () -> createService.create(
				new CreateUserCommand("valid", "a@b.co", null, "5215512345678", Set.of(12L))));

		verify(userRepository, never()).save(any());
		verify(notifier, never()).sendCode(any(), any());
	}

	private void existingTarget(String phone, String password) {
		when(userRepository.findById(TARGET)).thenReturn(Optional.of(User.builder().id(TARGET).username("t")
				.email("t@example.com").phone(phone).password(password).active(true).roles(Set.of(userRole)).build()));
	}

	@Test
	void resetClearsThePasswordRevokesSessionsClearsLockAndSendsAResetCode() {
		existingTarget("5215500000000", "old-hash");
		AccountBlocked blocked = AccountBlocked.create(TARGET);
		when(accountBlocked.findByUserId(TARGET)).thenReturn(Optional.of(blocked));
		when(notifier.sendCode("5215500000000", CODE)).thenReturn(true);

		IssuedCodeResult result = resetService.reset(ACTOR, TARGET);

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<OneTimeCode> savedCode = ArgumentCaptor.forClass(OneTimeCode.class);
		verify(userRepository).save(savedUser.capture());
		verify(codes).consumePending(TARGET, NOW);
		verify(codes).save(savedCode.capture());
		assertNull(savedUser.getValue().getPassword());
		assertEquals(OneTimeCodePurpose.PASSWORD_RESET, savedCode.getValue().getPurpose());
		verify(tokens).revokeAllByUserId(TARGET);
		verify(accountBlocked).resetAttempts(blocked);
		assertTrue(result.delivered());
	}

	@Test
	void resendingToAnAccountThatNeverHadAPasswordKeepsTheActivationPurpose() {
		existingTarget("5215500000000", null);
		when(accountBlocked.findByUserId(TARGET)).thenReturn(Optional.empty());

		resetService.reset(ACTOR, TARGET);

		ArgumentCaptor<OneTimeCode> savedCode = ArgumentCaptor.forClass(OneTimeCode.class);
		verify(codes).save(savedCode.capture());
		assertEquals(OneTimeCodePurpose.ACCOUNT_ACTIVATION, savedCode.getValue().getPurpose());
	}

	@Test
	void resetByYourselfIsRefused() {
		assertThrows(SelfModificationException.class, () -> resetService.reset(TARGET, TARGET));

		verify(userRepository, never()).save(any());
		verify(notifier, never()).sendCode(any(), any());
	}

	@Test
	void resetNeedsAPhoneNumber() {
		existingTarget(null, "old-hash");

		assertThrows(InvalidFieldException.class, () -> resetService.reset(ACTOR, TARGET));

		verify(userRepository, never()).save(any());
		verify(tokens, never()).revokeAllByUserId(anyLong());
	}

	@Test
	void failedDeliveryOnResetStillReportsNotDelivered() {
		existingTarget("5215500000000", "old-hash");
		when(accountBlocked.findByUserId(TARGET)).thenReturn(Optional.empty());
		when(notifier.sendCode(anyString(), anyString())).thenReturn(false);

		assertFalse(resetService.reset(ACTOR, TARGET).delivered());
	}
}
