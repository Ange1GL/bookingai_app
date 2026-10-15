package com.github.angellariosacosta.bookingapp.domain.exception;

public class CompanyNotFoundException extends RuntimeException {

	public CompanyNotFoundException(String message) {
		super(message);
	}
}
