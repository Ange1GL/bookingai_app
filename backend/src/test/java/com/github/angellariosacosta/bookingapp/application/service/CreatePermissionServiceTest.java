package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.angellariosacosta.bookingapp.application.command.CreatePermissionCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePermissionNameException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

class CreatePermissionServiceTest {

	private PermissionRepositoryPort repository;
	private CreatePermissionService service;

	@BeforeEach
	void setUp() {
		repository = mock(PermissionRepositoryPort.class);
		service = new CreatePermissionService(repository);
	}

	@Test
	void savesNormalizedActiveNonSystemPermission() {
		when(repository.existsByName("REPORT_READ")).thenReturn(false);
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(new CreatePermissionCommand("report_read", "Read reports"));

		ArgumentCaptor<Permission> saved = ArgumentCaptor.forClass(Permission.class);
		verify(repository).save(saved.capture());
		assertEquals(new Permission(null, "REPORT_READ", "Read reports", true, false), saved.getValue());
	}

	@Test
	void rejectsDuplicateName() {
		when(repository.existsByName("REPORT_READ")).thenReturn(true);

		assertThrows(DuplicatePermissionNameException.class,
				() -> service.create(new CreatePermissionCommand("REPORT_READ", null)));

		verify(repository, never()).save(any());
	}
}
