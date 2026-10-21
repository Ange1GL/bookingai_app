package com.github.angellariosacosta.bookingapp.domain.exception;

public class DuplicatePhoneException extends RuntimeException {

	public DuplicatePhoneException(String message) {
		super(message);
	}
}
