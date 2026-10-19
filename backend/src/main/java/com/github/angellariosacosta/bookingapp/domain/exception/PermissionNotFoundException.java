package com.github.angellariosacosta.bookingapp.domain.exception;

public class PermissionNotFoundException extends RuntimeException {

	public PermissionNotFoundException(String message) {
		super(message);
	}
}
