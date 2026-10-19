package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SecureRandomTemporaryPasswordGeneratorTest {

	private final SecureRandomTemporaryPasswordGenerator generator = new SecureRandomTemporaryPasswordGenerator();

	private static boolean containsAnyOf(String password, String alphabet) {
		return password.chars().anyMatch(c -> alphabet.indexOf(c) >= 0);
	}

	@Test
	void alwaysHasExpectedLengthAndEveryCharacterClass() {
		for (int i = 0; i < 500; i++) {
			String password = generator.generate();

			assertEquals(SecureRandomTemporaryPasswordGenerator.LENGTH, password.length());
			assertTrue(containsAnyOf(password, SecureRandomTemporaryPasswordGenerator.UPPER), password);
			assertTrue(containsAnyOf(password, SecureRandomTemporaryPasswordGenerator.LOWER), password);
			assertTrue(containsAnyOf(password, SecureRandomTemporaryPasswordGenerator.DIGITS), password);
			assertTrue(containsAnyOf(password, SecureRandomTemporaryPasswordGenerator.SYMBOLS), password);
		}
	}

	@Test
	void neverUsesAmbiguousCharacters() {
		for (int i = 0; i < 500; i++) {
			assertTrue(generator.generate().chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0));
		}
	}

	@Test
	void generatesDifferentPasswords() {
		Set<String> passwords = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			passwords.add(generator.generate());
		}

		assertNotEquals(1, passwords.size());
		assertEquals(200, passwords.size());
	}
}
