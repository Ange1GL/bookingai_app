package com.github.angellariosacosta.bookingapp.domain.shared;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

class PasswordPolicyTest {

	@ParameterizedTest
	@ValueSource(strings = {"abcdefghij1", "Abc12345!x", "pássword123", "1234567890a"})
	void acceptsStrongEnoughPasswords(String password) {
		assertDoesNotThrow(() -> PasswordPolicy.validate(password));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "short1A", "abcdefghijk", "1234567890123", "!!!!!!!!!!!!"})
	void rejectsTooShortOrMissingLetterOrDigit(String password) {
		assertThrows(InvalidFieldException.class, () -> PasswordPolicy.validate(password));
	}

	@org.junit.jupiter.api.Test
	void rejectsNullAndOverBcryptLimit() {
		assertThrows(InvalidFieldException.class, () -> PasswordPolicy.validate(null));
		assertThrows(InvalidFieldException.class,
				() -> PasswordPolicy.validate("a1".repeat(PasswordPolicy.MAX_LENGTH)));
	}

	@Test
	void acceptsExactlyTheMaximumLength() {
		assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MAX_LENGTH - 1) + "1"));
	}
}
