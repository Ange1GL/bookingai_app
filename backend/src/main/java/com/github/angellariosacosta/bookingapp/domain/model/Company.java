package com.github.angellariosacosta.bookingapp.domain.model;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import lombok.Getter;

/** Negocio de citas de un usuario: relación uno a uno, el usuario dueño del tenant es su identidad. */
@Getter
public class Company {

	public static final int MAX_NAME_LENGTH = 100;

	private final Long userId;
	private final String name;

	public Company(Long userId, String name) {
		if (userId == null || userId <= 0) {
			throw new InvalidFieldException("userId", "must be greater than 0");
		}
		if (name == null || name.isBlank()) {
			throw new InvalidFieldException("name", "must not be null or blank");
		}
		if (name.strip().length() > MAX_NAME_LENGTH) {
			throw new InvalidFieldException("name", "must not exceed " + MAX_NAME_LENGTH + " characters");
		}
		this.userId = userId;
		this.name = name.strip();
	}
}
