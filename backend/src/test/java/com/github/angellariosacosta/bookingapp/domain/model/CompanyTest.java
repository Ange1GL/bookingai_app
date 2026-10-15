package com.github.angellariosacosta.bookingapp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

class CompanyTest {

	@DisplayName("Should create a company identified by its owner with a stripped name")
	@Test
	void shouldCreateCompany() {
		Company company = new Company(1L, "  Barbería  ");

		assertEquals(1L, company.getUserId());
		assertEquals("Barbería", company.getName());
	}

	@DisplayName("Should reject a blank name")
	@Test
	void shouldRejectBlankName() {
		assertThrows(InvalidFieldException.class, () -> new Company(1L, " "));
	}

	@DisplayName("Should reject a name longer than the maximum")
	@Test
	void shouldRejectTooLongName() {
		String tooLong = "a".repeat(Company.MAX_NAME_LENGTH + 1);

		assertThrows(InvalidFieldException.class, () -> new Company(1L, tooLong));
	}

	@DisplayName("Should reject a missing or invalid owner")
	@Test
	void shouldRejectInvalidOwner() {
		assertThrows(InvalidFieldException.class, () -> new Company(null, "Barbería"));
		assertThrows(InvalidFieldException.class, () -> new Company(0L, "Barbería"));
	}
}
