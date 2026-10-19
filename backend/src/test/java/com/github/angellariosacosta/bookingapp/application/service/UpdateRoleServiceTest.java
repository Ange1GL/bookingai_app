package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.command.UpdateRoleCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicateRoleNameException;
import com.github.angellariosacosta.bookingapp.domain.exception.RoleNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemRoleProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

class UpdateRoleServiceTest {

	private static final long ID = 4L;

	private RoleRepository repository;
	private UpdateRoleService service;

	@BeforeEach
	void setUp() {
		repository = mock(RoleRepository.class);
		service = new UpdateRoleService(new RoleFinder(repository), repository);
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private void existing(String name, boolean system) {
		when(repository.findById(ID)).thenReturn(Optional.of(new Role(ID, name, "old", true, system, Set.of())));
	}

	@Test
	void renamesCustomRole() {
		existing("STAFF", false);
		when(repository.existsByName("TEAM")).thenReturn(false);

		Role updated = service.update(new UpdateRoleCommand(ID, "team", "new"));

		assertEquals("TEAM", updated.name());
		assertEquals("new", updated.description());
	}

	@Test
	void refusesRenamingSystemRole() {
		existing("ADMIN", true);

		assertThrows(SystemRoleProtectedException.class, () -> service.update(new UpdateRoleCommand(ID, "BOSS", null)));

		verify(repository, never()).save(any());
	}

	@Test
	void allowsEditingDescriptionOfSystemRole() {
		existing("ADMIN", true);

		assertEquals("new", service.update(new UpdateRoleCommand(ID, "admin", "new")).description());
	}

	@Test
	void refusesNameTakenByAnotherRole() {
		existing("STAFF", false);
		when(repository.existsByName("TEAM")).thenReturn(true);

		assertThrows(DuplicateRoleNameException.class, () -> service.update(new UpdateRoleCommand(ID, "TEAM", null)));
	}

	@Test
	void keepingTheSameNameDoesNotCheckDuplicates() {
		existing("STAFF", false);

		service.update(new UpdateRoleCommand(ID, "staff", "other"));

		verify(repository, never()).existsByName(any());
	}

	@Test
	void reportsMissingRole() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThrows(RoleNotFoundException.class, () -> service.update(new UpdateRoleCommand(ID, "X", null)));
	}
}
