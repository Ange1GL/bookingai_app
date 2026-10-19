package com.github.angellariosacosta.bookingapp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

class PermissionTest {

	@Test
	void createNormalizesNameAndDescription() {
		Permission permission = Permission.create("  report_read ", "  Read reports  ");

		assertNull(permission.id());
		assertEquals("REPORT_READ", permission.name());
		assertEquals("Read reports", permission.description());
		assertTrue(permission.active());
		assertFalse(permission.system());
	}

	@Test
	void blankDescriptionBecomesNull() {
		assertNull(Permission.create("REPORT_READ", "   ").description());
	}

	@Test
	void rejectsBlankName() {
		assertThrows(InvalidFieldException.class, () -> Permission.create("  ", null));
		assertThrows(InvalidFieldException.class, () -> Permission.create(null, null));
	}

	@Test
	void rejectsNameThatIsNotUpperSnakeCase() {
		assertThrows(InvalidFieldException.class, () -> Permission.create("report read", null));
		assertThrows(InvalidFieldException.class, () -> Permission.create("1REPORT", null));
		assertThrows(InvalidFieldException.class, () -> Permission.create("REPORT-READ", null));
	}

	@Test
	void rejectsTooLongNameAndDescription() {
		assertThrows(InvalidFieldException.class,
				() -> Permission.create("A".repeat(Permission.MAX_NAME_LENGTH + 1), null));
		assertThrows(InvalidFieldException.class,
				() -> Permission.create("REPORT_READ", "x".repeat(Permission.MAX_DESCRIPTION_LENGTH + 1)));
	}

	@Test
	void withersKeepIdentity() {
		Permission original = new Permission(5L, "REPORT_READ", "d", true, true);

		Permission changed = original.withActive(false).withDescription("new");

		assertEquals(5L, changed.id());
		assertEquals("REPORT_READ", changed.name());
		assertEquals("new", changed.description());
		assertFalse(changed.active());
		assertTrue(changed.system());
	}
}
