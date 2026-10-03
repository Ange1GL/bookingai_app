package com.github.angellariosacosta.bookingapp.domain.exception;

public class InvalidStatusTransitionException extends RuntimeException {

	public InvalidStatusTransitionException(String message) {
		super(message);
	}
}
