package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.command.AssignUserRolesCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.LastAdminProtectedException;
import com.github.angellariosacosta.bookingapp.domain.exception.SelfModificationException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

class AssignUserRolesServiceTest {

	private static final long ACTOR = 1L;
	private static final long TARGET = 2L;

	private final Role admin = new Role(10L, Role.ADMIN_NAME, null, true, true, Set.of());
	private final Role userRole = new Role(11L, Role.USER_NAME, null, true, true, Set.of());
	private final Role disabled = new Role(12L, "OLD", null, false, false, Set.of());

	private UserRepository userRepository;
	private RoleRepository roleRepository;
	private AssignUserRolesService service;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		roleRepository = mock(RoleRepository.class);
		service = new AssignUserRolesService(new UserFinder(userRepository), new UserAdminGuard(userRepository),
				new AssignableRoles(roleRepository), userRepository);
	}

	private void target(Role... roles) {
		User user = User.builder().id(TARGET).username("t").active(true).roles(Set.of(roles)).build();
		when(userRepository.findById(TARGET)).thenReturn(Optional.of(user));
	}

	@Test
	void replacesRoles() {
		target(userRole);
		when(roleRepository.findAllByIds(Set.of(10L, 11L))).thenReturn(List.of(admin, userRole));

		service.assign(new AssignUserRolesCommand(ACTOR, TARGET, Set.of(10L, 11L)));

		verify(userRepository).replaceRoles(TARGET, Set.of(10L, 11L));
	}

	@Test
	void requiresAtLeastOneRole() {
		target(userRole);

		assertThrows(InvalidFieldException.class, () -> service.assign(new AssignUserRolesCommand(ACTOR, TARGET, Set.of())));
		assertThrows(InvalidFieldException.class, () -> service.assign(new AssignUserRolesCommand(ACTOR, TARGET, null)));
	}

	@Test
	void rejectsUnknownRoles() {
		target(userRole);
		when(roleRepository.findAllByIds(Set.of(11L, 99L))).thenReturn(List.of(userRole));

		assertThrows(InvalidFieldException.class,
				() -> service.assign(new AssignUserRolesCommand(ACTOR, TARGET, Set.of(11L, 99L))));
		verify(userRepository, never()).replaceRoles(any(), any());
	}

	@Test
	void rejectsDisabledRoles() {
		target(userRole);
		when(roleRepository.findAllByIds(Set.of(12L))).thenReturn(List.of(disabled));

		assertThrows(InvalidFieldException.class, () -> service.assign(new AssignUserRolesCommand(ACTOR, TARGET, Set.of(12L))));
	}

	@Test
	void adminCannotRemoveOwnAdminRole() {
		target(admin);
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));

		assertThrows(SelfModificationException.class,
				() -> service.assign(new AssignUserRolesCommand(TARGET, TARGET, Set.of(11L))));
		verify(userRepository, never()).replaceRoles(any(), any());
	}

	@Test
	void cannotRemoveAdminFromTheLastActiveAdmin() {
		target(admin);
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));
		when(userRepository.countActiveUsersWithRole(Role.ADMIN_NAME, TARGET)).thenReturn(0L);

		assertThrows(LastAdminProtectedException.class,
				() -> service.assign(new AssignUserRolesCommand(ACTOR, TARGET, Set.of(11L))));
		verify(userRepository, never()).replaceRoles(any(), any());
	}

	@Test
	void canRemoveAdminWhenAnotherAdminRemains() {
		target(admin);
		when(roleRepository.findAllByIds(Set.of(11L))).thenReturn(List.of(userRole));
		when(userRepository.countActiveUsersWithRole(Role.ADMIN_NAME, TARGET)).thenReturn(1L);

		service.assign(new AssignUserRolesCommand(ACTOR, TARGET, Set.of(11L)));

		verify(userRepository).replaceRoles(TARGET, Set.of(11L));
	}
}
