package com.github.angellariosacosta.bookingapp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

class RoleTest {

	@Test
	void createNormalizesAndStartsActiveWithoutPermissions() {
		Role role = Role.create(" receptionist ", "  Front desk ");

		assertNull(role.id());
		assertEquals("RECEPTIONIST", role.name());
		assertEquals("Front desk", role.description());
		assertTrue(role.active());
		assertFalse(role.system());
		assertTrue(role.permissions().isEmpty());
	}

	@Test
	void rejectsInvalidNames() {
		assertThrows(InvalidFieldException.class, () -> Role.create(" ", null));
		assertThrows(InvalidFieldException.class, () -> Role.create("front desk", null));
		assertThrows(InvalidFieldException.class, () -> Role.create("A".repeat(Role.MAX_NAME_LENGTH + 1), null));
	}

	@Test
	void withersKeepIdentityAndPermissions() {
		Permission permission = new Permission(1L, "USER_READ", null, true, true);
		Role original = new Role(3L, "STAFF", "d", true, false, Set.of(permission));

		Role changed = original.withName("team").withDescription("new").withActive(false);

		assertEquals(3L, changed.id());
		assertEquals("TEAM", changed.name());
		assertEquals("new", changed.description());
		assertFalse(changed.active());
		assertEquals(Set.of(permission), changed.permissions());
	}

	@Test
	void identifiesAdminRole() {
		assertTrue(new Role(1L, Role.ADMIN_NAME, null, true, true, Set.of()).isAdmin());
		assertFalse(new Role(2L, Role.USER_NAME, null, true, true, Set.of()).isAdmin());
	}
}
