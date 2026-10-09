package com.github.angellariosacosta.bookingapp.domain.exception;

public class NoShowNotAllowedException extends RuntimeException {

	public NoShowNotAllowedException(String message) {
		super(message);
	}
}
