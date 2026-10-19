package com.github.angellariosacosta.bookingapp.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

class UsernamesTest {

	@Test
	void stripsSurroundingWhitespaceAndKeepsCase() {
		assertEquals("Angel.L_1-x", Usernames.normalize("  Angel.L_1-x "));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "  ", "ab", "has space", "ñandú", "a@b.com", "semi;colon"})
	void rejectsInvalidUsernames(String raw) {
		assertThrows(InvalidFieldException.class, () -> Usernames.normalize(raw));
	}

	@Test
	void rejectsNullAndTooLong() {
		assertThrows(InvalidFieldException.class, () -> Usernames.normalize(null));
		assertThrows(InvalidFieldException.class, () -> Usernames.normalize("a".repeat(Usernames.MAX_LENGTH + 1)));
	}
}
