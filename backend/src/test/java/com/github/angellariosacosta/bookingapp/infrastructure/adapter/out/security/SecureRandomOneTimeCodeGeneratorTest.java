package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SecureRandomOneTimeCodeGeneratorTest {

	private final SecureRandomOneTimeCodeGenerator generator = new SecureRandomOneTimeCodeGenerator();

	@Test
	void alwaysProducesExactlySixDigitsKeepingLeadingZeros() {
		boolean sawLeadingZero = false;
		for (int i = 0; i < 5000; i++) {
			String code = generator.generate();

			assertEquals(SecureRandomOneTimeCodeGenerator.LENGTH, code.length());
			assertTrue(code.chars().allMatch(Character::isDigit), code);
			sawLeadingZero |= code.startsWith("0");
		}
		assertTrue(sawLeadingZero, "con 5000 códigos debería salir alguno que empiece en 0");
	}

	@Test
	void producesVariedCodes() {
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			codes.add(generator.generate());
		}

		assertTrue(codes.size() > 150);
	}
}
