package com.github.angellariosacosta.bookingapp.domain.shared;

import java.util.regex.Pattern;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

/** Reglas del username: es el identificador estable de login (y del JWT), por eso es estricto e inmutable. */
public final class Usernames {

	public static final int MIN_LENGTH = 3;
	public static final int MAX_LENGTH = 50;
	private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9._-]+");

	private Usernames() {
	}

	public static String normalize(String rawUsername) {
		if (rawUsername == null || rawUsername.isBlank()) {
			throw new InvalidFieldException("username", "must not be null or blank");
		}
		String username = rawUsername.strip();
		if (username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) {
			throw new InvalidFieldException("username",
					"must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
		}
		if (!ALLOWED.matcher(username).matches()) {
			throw new InvalidFieldException("username", "may only contain letters, digits, dots, dashes and underscores");
		}
		return username;
	}
}
