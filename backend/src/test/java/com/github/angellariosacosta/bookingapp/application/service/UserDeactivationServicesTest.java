package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.LastAdminProtectedException;
import com.github.angellariosacosta.bookingapp.domain.exception.SelfModificationException;
import com.github.angellariosacosta.bookingapp.domain.exception.UserNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

/** Disable y delete comparten guard: se prueban juntos las mismas reglas. */
class UserDeactivationServicesTest {

	private static final long ACTOR = 1L;
	private static final long TARGET = 2L;

	private final Role admin = new Role(10L, Role.ADMIN_NAME, null, true, true, Set.of());
	private final Role userRole = new Role(11L, Role.USER_NAME, null, true, true, Set.of());

	private UserRepository userRepository;
	private RefreshTokenRepositoryPort tokens;
	private ChangeUserStatusService changeStatus;
	private DeleteUserService delete;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		tokens = mock(RefreshTokenRepositoryPort.class);
		UserFinder finder = new UserFinder(userRepository);
		UserAdminGuard guard = new UserAdminGuard(userRepository);
		changeStatus = new ChangeUserStatusService(finder, guard, userRepository, tokens);
		delete = new DeleteUserService(finder, guard, userRepository, tokens);
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private void target(boolean active, Role... roles) {
		when(userRepository.findById(TARGET)).thenReturn(Optional.of(
				User.builder().id(TARGET).username("t").active(active).roles(Set.of(roles)).build()));
	}

	@Test
	void disablesUserAndRevokesSessions() {
		target(true, userRole);

		assertFalse(changeStatus.disable(ACTOR, TARGET).isActive());

		verify(tokens).revokeAllByUserId(TARGET);
	}

	@Test
	void cannotDisableYourself() {
		assertThrows(SelfModificationException.class, () -> changeStatus.disable(TARGET, TARGET));

		verify(userRepository, never()).save(any());
		verify(tokens, never()).revokeAllByUserId(anyLong());
	}

	@Test
	void cannotDisableTheLastActiveAdmin() {
		target(true, admin);
		when(userRepository.countActiveUsersWithRole(Role.ADMIN_NAME, TARGET)).thenReturn(0L);

		assertThrows(LastAdminProtectedException.class, () -> changeStatus.disable(ACTOR, TARGET));

		verify(userRepository, never()).save(any());
		verify(tokens, never()).revokeAllByUserId(anyLong());
	}

	@Test
	void canDisableAdminWhenAnotherRemains() {
		target(true, admin);
		when(userRepository.countActiveUsersWithRole(Role.ADMIN_NAME, TARGET)).thenReturn(2L);

		assertFalse(changeStatus.disable(ACTOR, TARGET).isActive());
	}

	@Test
	void disablingInactiveUserIsIdempotent() {
		target(false, admin);

		assertFalse(changeStatus.disable(ACTOR, TARGET).isActive());

		verify(userRepository, never()).save(any());
		verify(userRepository, never()).countActiveUsersWithRole(any(), any());
	}

	@Test
	void enablesInactiveUser() {
		target(false, userRole);

		assertTrue(changeStatus.enable(TARGET).isActive());
	}

	@Test
	void deletesUserAndRevokesSessions() {
		target(true, userRole);

		delete.delete(ACTOR, TARGET);

		verify(userRepository).markDeleted(TARGET);
		verify(tokens).revokeAllByUserId(TARGET);
	}

	@Test
	void cannotDeleteYourself() {
		assertThrows(SelfModificationException.class, () -> delete.delete(TARGET, TARGET));

		verify(userRepository, never()).markDeleted(anyLong());
	}

	@Test
	void cannotDeleteTheLastActiveAdmin() {
		target(true, admin);
		when(userRepository.countActiveUsersWithRole(Role.ADMIN_NAME, TARGET)).thenReturn(0L);

		assertThrows(LastAdminProtectedException.class, () -> delete.delete(ACTOR, TARGET));

		verify(userRepository, never()).markDeleted(anyLong());
	}

	@Test
	void reportsMissingUser() {
		when(userRepository.findById(TARGET)).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> delete.delete(ACTOR, TARGET));
	}
}
