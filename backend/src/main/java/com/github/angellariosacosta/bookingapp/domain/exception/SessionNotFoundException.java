package com.github.angellariosacosta.bookingapp.domain.exception;

public class SessionNotFoundException extends RuntimeException {

	public SessionNotFoundException(String message) {
		super(message);
	}
}
