package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.PermissionInUseException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemPermissionProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

class DeletePermissionServiceTest {

	private static final long ID = 9L;

	private PermissionRepositoryPort repository;
	private DeletePermissionService service;

	@BeforeEach
	void setUp() {
		repository = mock(PermissionRepositoryPort.class);
		service = new DeletePermissionService(
				new PermissionFinder(repository), new PermissionDeactivationGuard(repository), repository);
	}

	@Test
	void logicallyDeletesUnusedCustomPermission() {
		when(repository.findById(ID)).thenReturn(Optional.of(new Permission(ID, "REPORT_READ", null, true, false)));
		when(repository.isAssignedToRole(ID)).thenReturn(false);

		service.delete(ID);

		verify(repository).markDeleted(ID);
	}

	@Test
	void refusesPermissionInUse() {
		when(repository.findById(ID)).thenReturn(Optional.of(new Permission(ID, "REPORT_READ", null, true, false)));
		when(repository.isAssignedToRole(ID)).thenReturn(true);

		assertThrows(PermissionInUseException.class, () -> service.delete(ID));

		verify(repository, never()).markDeleted(anyLong());
	}

	@Test
	void refusesSystemPermission() {
		when(repository.findById(ID)).thenReturn(Optional.of(new Permission(ID, "USER_READ", null, true, true)));

		assertThrows(SystemPermissionProtectedException.class, () -> service.delete(ID));

		verify(repository, never()).markDeleted(anyLong());
	}
}
