package com.github.angellariosacosta.bookingapp.domain.exception;

public class SystemPermissionProtectedException extends RuntimeException {

	public SystemPermissionProtectedException(String message) {
		super(message);
	}
}
