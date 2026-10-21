package com.github.angellariosacosta.bookingapp.domain.shared;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

/** Política de contraseñas definidas por el usuario (las temporales las genera el sistema y ya la cumplen). */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 10;
	// BCrypt solo usa los primeros 72 bytes: aceptar más daría una falsa sensación de seguridad.
	public static final int MAX_LENGTH = 72;
	private static final String FIELD = "newPassword";

	private PasswordPolicy() {
	}

	public static void validate(String password) {
		if (password == null || password.length() < MIN_LENGTH) {
			throw new InvalidFieldException(FIELD, "must have at least " + MIN_LENGTH + " characters");
		}
		if (password.length() > MAX_LENGTH) {
			throw new InvalidFieldException(FIELD, "must not exceed " + MAX_LENGTH + " characters");
		}
		if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
			throw new InvalidFieldException(FIELD, "must contain at least one letter and one digit");
		}
	}
}
