package com.github.angellariosacosta.bookingapp.domain.exception;

public class NoShowAlreadyRegisteredException extends RuntimeException {

	public NoShowAlreadyRegisteredException(String message) {
		super(message);
	}
}
