package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.TemporaryPasswordGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.UserCredentialNotifierPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCredentialsResult;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePhoneException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.SelfModificationException;
import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

class TemporaryCredentialsServicesTest {

	private static final String TEMP = "Tmp-Pass#12";
	private static final long ACTOR = 1L;
	private static final long TARGET = 9L;

	private final Role userRole = new Role(11L, Role.USER_NAME, null, true, true, Set.of());
	private final Role disabledRole = new Role(12L, "OLD", null, false, false, Set.of());

	private UserRepository userRepository;
	private RoleRepository roleRepository;
	private PasswordHasher hasher;
	private RefreshTokenRepositoryPort tokens;
	private AccountBlockedRepository accountBlocked;
	private UserCredentialNotifierPort notifier;
	private CreateUserService createService;
	private ResetUserPasswordService resetService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		roleRepository = mock(RoleRepository.class);
		hasher = mock(PasswordHasher.class);
		tokens = mock(RefreshTokenRepositoryPort.class);
		accountBlocked = mock(AccountBlockedRepository.class);
		notifier = mock(UserCredentialNotifierPort.class);
		TemporaryPasswordGenerator generator = () -> TEMP;
		when(hasher.hash(TEMP)).thenReturn("hashed");
		when(userRepository.save(any())).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			if (user.getId() == null) {
				user.setId(TARGET);
			}
			return user;
		});

		UserFinder finder = new UserFinder(userRepository);
		TemporaryCredentialsPersister persister = new TemporaryCredentialsPersister(userRepository, finder,
				new UserAdminGuard(userRepository), new AssignableRoles(roleRepository), generator, hasher, tokens,
				accountBlocked);
		CredentialsDelivery delivery = new CredentialsDelivery(notifier);
		createService = new CreateUserService(persister, delivery);
		resetService = new ResetUserPasswordService(persister, delivery);
	}

	private CreateUserCommand command() {
		return new CreateUserCommand(" new.user ", " new@example.com ", " New ", "+52 155 1234 5678", Set.of(11L));
	}

	@Test
	void createsUserWithHashedTemporaryPasswordThenSendsIt() {
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));
		when(notifier.sendTemporaryPassword(anyString(), anyString(), anyString())).thenReturn(true);

		IssuedCredentialsResult result = createService.create(command());

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		InOrder order = inOrder(userRepository, notifier);
		order.verify(userRepository).save(saved.capture());
		order.verify(notifier).sendTemporaryPassword("5215512345678", "new.user", TEMP);

		User user = saved.getValue();
		assertEquals("new.user", user.getUsername());
		assertEquals("new@example.com", user.getEmail());
		assertEquals("New", user.getName());
		assertEquals("hashed", user.getPassword());
		assertTrue(user.isMustChangePassword());
		assertTrue(user.isActive());
		assertEquals(Set.of(userRole), user.getRoles());
		assertTrue(result.delivered());
		assertEquals(TEMP, result.temporaryPassword());
	}

	@Test
	void userIsStillCreatedWhenWhatsAppFails() {
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));
		when(notifier.sendTemporaryPassword(anyString(), anyString(), anyString())).thenReturn(false);

		IssuedCredentialsResult result = createService.create(command());

		assertFalse(result.delivered());
		assertEquals(TARGET, result.user().getId());
	}

	@Test
	void resultToStringNeverContainsThePassword() {
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));

		assertFalse(createService.create(command()).toString().contains(TEMP));
	}

	@Test
	void rejectsDuplicateUsernameEmailAndPhoneWithoutSending() {
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
		verify(notifier, never()).sendTemporaryPassword(any(), any(), any());
	}

	@Test
	void rejectsInvalidInputsAndDisabledRolesWithoutSending() {
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
		verify(notifier, never()).sendTemporaryPassword(any(), any(), any());
	}

	private void existingTarget(String phone) {
		when(userRepository.findById(TARGET)).thenReturn(Optional.of(User.builder().id(TARGET).username("t")
				.email("t@example.com").phone(phone).password("old").active(true).roles(Set.of(userRole)).build()));
	}

	@Test
	void resetRegeneratesPasswordRevokesSessionsClearsLockAndSends() {
		existingTarget("5215500000000");
		AccountBlocked blocked = AccountBlocked.create(TARGET);
		when(accountBlocked.findByUserId(TARGET)).thenReturn(Optional.of(blocked));
		when(notifier.sendTemporaryPassword("5215500000000", "t", TEMP)).thenReturn(true);

		IssuedCredentialsResult result = resetService.reset(ACTOR, TARGET);

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertEquals("hashed", saved.getValue().getPassword());
		assertTrue(saved.getValue().isMustChangePassword());
		verify(tokens).revokeAllByUserId(TARGET);
		verify(accountBlocked).resetAttempts(blocked);
		assertTrue(result.delivered());
	}

	@Test
	void resetByYourselfIsRefused() {
		assertThrows(SelfModificationException.class, () -> resetService.reset(TARGET, TARGET));

		verify(userRepository, never()).save(any());
		verify(notifier, never()).sendTemporaryPassword(any(), any(), any());
	}

	@Test
	void resetNeedsAPhoneNumber() {
		existingTarget(null);

		assertThrows(InvalidFieldException.class, () -> resetService.reset(ACTOR, TARGET));

		verify(userRepository, never()).save(any());
		verify(tokens, never()).revokeAllByUserId(anyLong());
	}

	@Test
	void failedDeliveryOnResetStillReportsNotDelivered() {
		existingTarget("5215500000000");
		when(accountBlocked.findByUserId(TARGET)).thenReturn(Optional.empty());
		when(notifier.sendTemporaryPassword(anyString(), anyString(), anyString())).thenReturn(false);

		assertFalse(resetService.reset(ACTOR, TARGET).delivered());
	}
}
