package com.github.angellariosacosta.bookingapp.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

/** Reglas de nombre y descripción compartidas por los catálogos administrables (roles y permisos). */
final class CatalogFields {

	static final int MAX_DESCRIPTION_LENGTH = 255;
	private static final Pattern NAME_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

	private CatalogFields() {
	}

	static String normalizeName(String rawName, int maxLength) {
		if (rawName == null || rawName.isBlank()) {
			throw new InvalidFieldException("name", "must not be null or blank");
		}
		String normalized = rawName.strip().toUpperCase(Locale.ROOT);
		if (normalized.length() > maxLength) {
			throw new InvalidFieldException("name", "must not exceed " + maxLength + " characters");
		}
		if (!NAME_PATTERN.matcher(normalized).matches()) {
			throw new InvalidFieldException("name", "must be UPPER_SNAKE_CASE (letters, digits and underscores)");
		}
		return normalized;
	}

	static String normalizeDescription(String rawDescription) {
		if (rawDescription == null || rawDescription.isBlank()) {
			return null;
		}
		String normalized = rawDescription.strip();
		if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
			throw new InvalidFieldException("description", "must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
		}
		return normalized;
	}
}
