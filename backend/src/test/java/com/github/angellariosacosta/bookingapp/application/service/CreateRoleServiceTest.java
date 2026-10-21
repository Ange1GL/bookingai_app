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

import com.github.angellariosacosta.bookingapp.application.command.CreateRoleCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicateRoleNameException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

class CreateRoleServiceTest {

	private RoleRepository roleRepository;
	private PermissionRepositoryPort permissionRepository;
	private CreateRoleService service;

	@BeforeEach
	void setUp() {
		roleRepository = mock(RoleRepository.class);
		permissionRepository = mock(PermissionRepositoryPort.class);
		service = new CreateRoleService(roleRepository,
				new RolePermissionsAssigner(permissionRepository, roleRepository), new RoleFinder(roleRepository));
	}

	@Test
	void createsRoleWithoutPermissions() {
		Role saved = new Role(5L, "STAFF", null, true, false, Set.of());
		when(roleRepository.existsByName("STAFF")).thenReturn(false);
		when(roleRepository.save(any())).thenReturn(saved);

		assertEquals(saved, service.create(new CreateRoleCommand("staff", null, null)));

		verify(roleRepository, never()).replacePermissions(any(), any());
	}

	@Test
	void createsRoleAndAssignsPermissions() {
		Role saved = new Role(5L, "STAFF", null, true, false, Set.of());
		Permission userRead = new Permission(1L, "USER_READ", null, true, true);
		Role reloaded = new Role(5L, "STAFF", null, true, false, Set.of(userRead));
		when(roleRepository.existsByName("STAFF")).thenReturn(false);
		when(roleRepository.save(any())).thenReturn(saved);
		when(permissionRepository.findAllByIds(Set.of(1L))).thenReturn(List.of(userRead));
		when(roleRepository.findById(5L)).thenReturn(Optional.of(reloaded));

		assertEquals(reloaded, service.create(new CreateRoleCommand("STAFF", null, Set.of(1L))));

		verify(roleRepository).replacePermissions(5L, Set.of(1L));
	}

	@Test
	void rejectsDuplicateName() {
		when(roleRepository.existsByName("STAFF")).thenReturn(true);

		assertThrows(DuplicateRoleNameException.class,
				() -> service.create(new CreateRoleCommand("STAFF", null, null)));

		verify(roleRepository, never()).save(any());
	}
}
