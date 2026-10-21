package com.github.angellariosacosta.bookingapp.domain.exception;

public class DuplicateRoleNameException extends RuntimeException {

	public DuplicateRoleNameException(String message) {
		super(message);
	}
}
