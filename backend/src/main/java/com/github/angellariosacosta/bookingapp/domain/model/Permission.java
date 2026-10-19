package com.github.angellariosacosta.bookingapp.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

public record Permission(Long id, String name, String description, boolean active, boolean system) {

	public static final int MAX_NAME_LENGTH = 100;
	public static final int MAX_DESCRIPTION_LENGTH = 255;
	private static final Pattern NAME_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

	/** Permiso nuevo: activo y nunca de sistema (el catálogo base solo se siembra por migración). */
	public static Permission create(String name, String description) {
		return new Permission(null, normalizeName(name), normalizeDescription(description), true, false);
	}

	public Permission withDescription(String newDescription) {
		return new Permission(id, name, normalizeDescription(newDescription), active, system);
	}

	public Permission withActive(boolean newActive) {
		return new Permission(id, name, description, newActive, system);
	}

	static String normalizeName(String rawName) {
		if (rawName == null || rawName.isBlank()) {
			throw new InvalidFieldException("name", "must not be null or blank");
		}
		String normalized = rawName.strip().toUpperCase(Locale.ROOT);
		if (normalized.length() > MAX_NAME_LENGTH) {
			throw new InvalidFieldException("name", "must not exceed " + MAX_NAME_LENGTH + " characters");
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
