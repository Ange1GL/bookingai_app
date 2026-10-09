package com.github.angellariosacosta.bookingapp.domain.exception;

public class InvalidFieldException extends RuntimeException {

	private final String field;

	public InvalidFieldException(String field, String reason) {
		super("Invalid field '" + field + "': " + reason);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
