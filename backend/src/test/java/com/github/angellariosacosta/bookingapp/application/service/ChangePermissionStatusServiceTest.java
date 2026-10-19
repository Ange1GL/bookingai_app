package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.PermissionInUseException;
import com.github.angellariosacosta.bookingapp.domain.exception.PermissionNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemPermissionProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

class ChangePermissionStatusServiceTest {

	private static final long ID = 7L;

	private PermissionRepositoryPort repository;
	private ChangePermissionStatusService service;

	@BeforeEach
	void setUp() {
		repository = mock(PermissionRepositoryPort.class);
		service = new ChangePermissionStatusService(
				new PermissionFinder(repository), new PermissionDeactivationGuard(repository), repository);
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void disablesUnusedCustomPermission() {
		when(repository.findById(ID)).thenReturn(Optional.of(new Permission(ID, "REPORT_READ", null, true, false)));
		when(repository.isAssignedToRole(ID)).thenReturn(false);

		assertFalse(service.disable(ID).active());
	}

	@Test
	void refusesToDisablePermissionInUse() {
		when(repository.findById(ID)).thenReturn(Optional.of(new Permission(ID, "REPORT_READ", null, true, false)));
		when(repository.isAssignedToRole(ID)).thenReturn(true);

		assertThrows(PermissionInUseException.class, () -> service.disable(ID));

		verify(repository, never()).save(any());
	}

	@Test
	void refusesToDisableSystemPermission() {
		when(repository.findById(ID)).thenReturn(Optional.of(new Permission(ID, "ADMIN_ALL", null, true, true)));

		assertThrows(SystemPermissionProtectedException.class, () -> service.disable(ID));

		verify(repository, never()).save(any());
	}

	@Test
	void disablingAnInactivePermissionIsIdempotent() {
		when(repository.findById(ID)).thenReturn(Optional.of(new Permission(ID, "REPORT_READ", null, false, false)));

		assertFalse(service.disable(ID).active());

		verify(repository, never()).isAssignedToRole(any());
		verify(repository, never()).save(any());
	}

	@Test
	void enablesInactivePermission() {
		when(repository.findById(ID)).thenReturn(Optional.of(new Permission(ID, "REPORT_READ", null, false, false)));

		assertTrue(service.enable(ID).active());
	}

	@Test
	void reportsMissingPermission() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		PermissionNotFoundException error = assertThrows(PermissionNotFoundException.class, () -> service.enable(ID));

		assertEquals("Permiso no encontrado: 7", error.getMessage());
	}
}
