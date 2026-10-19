package com.github.angellariosacosta.bookingapp.domain.exception;

public class SelfModificationException extends RuntimeException {

	public SelfModificationException(String message) {
		super(message);
	}
}
