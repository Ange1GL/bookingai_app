package com.github.angellariosacosta.bookingapp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

class CompanyTest {

	@DisplayName("Should create a new company with a stripped name and no id")
	@Test
	void shouldCreateNewCompany() {
		Company company = Company.createNew("  Barbería  ", 1L);

		assertNull(company.getId());
		assertEquals("Barbería", company.getName());
		assertEquals(1L, company.getUserId());
	}

	@DisplayName("Should reject a blank name")
	@Test
	void shouldRejectBlankName() {
		assertThrows(InvalidFieldException.class, () -> Company.createNew(" ", 1L));
	}

	@DisplayName("Should reject a name longer than the maximum")
	@Test
	void shouldRejectTooLongName() {
		String tooLong = "a".repeat(Company.MAX_NAME_LENGTH + 1);

		assertThrows(InvalidFieldException.class, () -> Company.createNew(tooLong, 1L));
	}

	@DisplayName("Should reject a missing or invalid owner")
	@Test
	void shouldRejectInvalidOwner() {
		assertThrows(InvalidFieldException.class, () -> Company.createNew("Barbería", null));
		assertThrows(InvalidFieldException.class, () -> Company.createNew("Barbería", 0L));
	}
}
