package com.github.angellariosacosta.bookingapp.domain.exception;

public class RoleInUseException extends RuntimeException {

	public RoleInUseException(String message) {
		super(message);
	}
}
