package com.github.angellariosacosta.bookingapp.domain.exception;

import lombok.Getter;

@Getter
public class InvalidFieldException extends RuntimeException {
	private final String field;

	public InvalidFieldException(String field, String reason) {
		super("Invalid field '" + field + "': " + reason);
		this.field = field;
	}

}
