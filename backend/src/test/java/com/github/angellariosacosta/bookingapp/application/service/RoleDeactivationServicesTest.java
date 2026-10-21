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

import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.RoleInUseException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemRoleProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

/** Disable y delete comparten guard: se prueban juntos las mismas reglas. */
class RoleDeactivationServicesTest {

	private static final long ID = 6L;

	private RoleRepository repository;
	private ChangeRoleStatusService changeStatus;
	private DeleteRoleService delete;

	@BeforeEach
	void setUp() {
		repository = mock(RoleRepository.class);
		RoleFinder finder = new RoleFinder(repository);
		RoleDeactivationGuard guard = new RoleDeactivationGuard(repository);
		changeStatus = new ChangeRoleStatusService(finder, guard, repository);
		delete = new DeleteRoleService(finder, guard, repository);
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private void existing(boolean active, boolean system) {
		when(repository.findById(ID)).thenReturn(Optional.of(new Role(ID, "STAFF", null, active, system, Set.of())));
	}

	@Test
	void disablesUnusedCustomRole() {
		existing(true, false);
		when(repository.isAssignedToUser(ID)).thenReturn(false);

		assertFalse(changeStatus.disable(ID).active());
	}

	@Test
	void refusesToDisableRoleInUse() {
		existing(true, false);
		when(repository.isAssignedToUser(ID)).thenReturn(true);

		assertThrows(RoleInUseException.class, () -> changeStatus.disable(ID));

		verify(repository, never()).save(any());
	}

	@Test
	void refusesToDisableSystemRole() {
		existing(true, true);

		assertThrows(SystemRoleProtectedException.class, () -> changeStatus.disable(ID));
	}

	@Test
	void disablingInactiveRoleIsIdempotent() {
		existing(false, false);

		assertFalse(changeStatus.disable(ID).active());

		verify(repository, never()).isAssignedToUser(anyLong());
	}

	@Test
	void enablesInactiveRole() {
		existing(false, false);

		assertTrue(changeStatus.enable(ID).active());
	}

	@Test
	void deletesUnusedCustomRole() {
		existing(true, false);

		delete.delete(ID);

		verify(repository).markDeleted(ID);
	}

	@Test
	void refusesToDeleteRoleInUse() {
		existing(true, false);
		when(repository.isAssignedToUser(ID)).thenReturn(true);

		assertThrows(RoleInUseException.class, () -> delete.delete(ID));

		verify(repository, never()).markDeleted(anyLong());
	}

	@Test
	void refusesToDeleteSystemRole() {
		existing(true, true);

		assertThrows(SystemRoleProtectedException.class, () -> delete.delete(ID));

		verify(repository, never()).markDeleted(anyLong());
	}
}
