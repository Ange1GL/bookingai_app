package com.github.angellariosacosta.bookingapp.domain.exception;

public class CustomerBlacklistedException extends RuntimeException {

	public CustomerBlacklistedException(String message) {
		super(message);
	}
}
