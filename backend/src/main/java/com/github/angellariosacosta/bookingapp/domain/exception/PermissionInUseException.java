package com.github.angellariosacosta.bookingapp.domain.exception;

public class PermissionInUseException extends RuntimeException {

	public PermissionInUseException(String message) {
		super(message);
	}
}
