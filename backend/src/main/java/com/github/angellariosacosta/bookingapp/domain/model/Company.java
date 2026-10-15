package com.github.angellariosacosta.bookingapp.domain.model;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import lombok.Getter;

@Getter
public class Company {

	public static final int MAX_NAME_LENGTH = 100;

	// null mientras la compañía no se ha guardado
	private final Long id;
	private final String name;
	// dueño de la compañía: todas las citas del negocio se agendan bajo este usuario
	private final Long userId;

	public Company(Long id, String name, Long userId) {
		if (id != null && id <= 0) {
			throw new InvalidFieldException("id", "must be greater than 0");
		}
		if (name == null || name.isBlank()) {
			throw new InvalidFieldException("name", "must not be null or blank");
		}
		if (name.strip().length() > MAX_NAME_LENGTH) {
			throw new InvalidFieldException("name", "must not exceed " + MAX_NAME_LENGTH + " characters");
		}
		if (userId == null || userId <= 0) {
			throw new InvalidFieldException("userId", "must be greater than 0");
		}
		this.id = id;
		this.name = name.strip();
		this.userId = userId;
	}

	public static Company createNew(String name, Long userId) {
		return new Company(null, name, userId);
	}
}
