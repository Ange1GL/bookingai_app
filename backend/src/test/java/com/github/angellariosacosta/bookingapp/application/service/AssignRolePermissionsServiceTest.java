package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.github.angellariosacosta.bookingapp.application.command.AssignRolePermissionsCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemRoleProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

class AssignRolePermissionsServiceTest {

	private static final long ROLE_ID = 2L;

	private final Permission userRead = new Permission(1L, "USER_READ", null, true, true);
	private final Permission adminAll = new Permission(7L, "ADMIN_ALL", null, true, true);
	private final Permission disabled = new Permission(9L, "OLD_PERMISSION", null, false, false);

	private RoleRepository roleRepository;
	private PermissionRepositoryPort permissionRepository;
	private AssignRolePermissionsService service;

	@BeforeEach
	void setUp() {
		roleRepository = mock(RoleRepository.class);
		permissionRepository = mock(PermissionRepositoryPort.class);
		service = new AssignRolePermissionsService(new RoleFinder(roleRepository),
				new RolePermissionsAssigner(permissionRepository, roleRepository));
	}

	private void existingRole(String name) {
		when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(new Role(ROLE_ID, name, null, true, true, Set.of())));
	}

	@Test
	void replacesPermissionSet() {
		existingRole("STAFF");
		when(permissionRepository.findAllByIds(Set.of(1L))).thenReturn(List.of(userRead));

		Role result = service.assign(new AssignRolePermissionsCommand(ROLE_ID, Set.of(1L)));

		verify(roleRepository).replacePermissions(ROLE_ID, Set.of(1L));
		assertEquals(ROLE_ID, result.id());
	}

	@Test
	void emptySetRevokesEverythingFromCustomRole() {
		existingRole("STAFF");
		when(permissionRepository.findAllByIds(Set.of())).thenReturn(List.of());

		service.assign(new AssignRolePermissionsCommand(ROLE_ID, Set.of()));

		verify(roleRepository).replacePermissions(ROLE_ID, Set.of());
	}

	@Test
	void rejectsUnknownPermissionIds() {
		existingRole("STAFF");
		when(permissionRepository.findAllByIds(Set.of(1L, 99L))).thenReturn(List.of(userRead));

		InvalidFieldException error = assertThrows(InvalidFieldException.class,
				() -> service.assign(new AssignRolePermissionsCommand(ROLE_ID, Set.of(1L, 99L))));

		assertEquals("permissionIds", error.getField());
		verify(roleRepository, never()).replacePermissions(any(), any());
	}

	@Test
	void rejectsDisabledPermissions() {
		existingRole("STAFF");
		when(permissionRepository.findAllByIds(Set.of(9L))).thenReturn(List.of(disabled));

		assertThrows(InvalidFieldException.class, () -> service.assign(new AssignRolePermissionsCommand(ROLE_ID, Set.of(9L))));

		verify(roleRepository, never()).replacePermissions(any(), any());
	}

	@Test
	void adminRoleCannotLoseAdminAll() {
		existingRole("ADMIN");
		when(permissionRepository.findAllByIds(Set.of(1L))).thenReturn(List.of(userRead));

		assertThrows(SystemRoleProtectedException.class,
				() -> service.assign(new AssignRolePermissionsCommand(ROLE_ID, Set.of(1L))));

		verify(roleRepository, never()).replacePermissions(any(), any());
	}

	@Test
	void adminRoleKeepingAdminAllIsAccepted() {
		existingRole("ADMIN");
		when(permissionRepository.findAllByIds(Set.of(1L, 7L))).thenReturn(List.of(userRead, adminAll));

		service.assign(new AssignRolePermissionsCommand(ROLE_ID, Set.of(1L, 7L)));

		verify(roleRepository).replacePermissions(ROLE_ID, Set.of(1L, 7L));
	}
}
