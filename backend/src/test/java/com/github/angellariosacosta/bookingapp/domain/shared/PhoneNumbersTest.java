package com.github.angellariosacosta.bookingapp.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

class PhoneNumbersTest {

	@Test
	void stripsPlusAndSeparators() {
		assertEquals("5215512345678", PhoneNumbers.normalize(" +52 (155) 1234-5678 "));
	}

	@Test
	void acceptsPlainDigits() {
		assertEquals("5215512345678", PhoneNumbers.normalize("5215512345678"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "1234567", "1234567890123456", "52abc5512345", "++5215512345678", "52-+155123456"})
	void rejectsInvalidNumbers(String raw) {
		assertThrows(InvalidFieldException.class, () -> PhoneNumbers.normalize(raw));
	}

	@Test
	void rejectsNull() {
		assertThrows(InvalidFieldException.class, () -> PhoneNumbers.normalize(null));
	}

	@Test
	void tenDigitNationalNumberGetsTheDefaultCountryCode() {
		assertEquals("525512345678", PhoneNumbers.normalizeWithDefaultCountry("55 1234 5678"));
	}

	@Test
	void plusPrefixAndLongerNumbersKeepTheirOwnCountry() {
		assertEquals("14155552671", PhoneNumbers.normalizeWithDefaultCountry("+1 415 555 2671"));
		assertEquals("525512345678", PhoneNumbers.normalizeWithDefaultCountry("525512345678"));
		assertEquals("5215512345678", PhoneNumbers.normalizeWithDefaultCountry("+52 1 55 1234 5678"));
	}

	@Test
	void defaultCountryNormalizationStillRejectsInvalidNumbers() {
		assertThrows(InvalidFieldException.class, () -> PhoneNumbers.normalizeWithDefaultCountry("abc"));
		assertThrows(InvalidFieldException.class, () -> PhoneNumbers.normalizeWithDefaultCountry(null));
	}
}
