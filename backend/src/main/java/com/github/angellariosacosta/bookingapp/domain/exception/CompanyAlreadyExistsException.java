package com.github.angellariosacosta.bookingapp.domain.exception;

public class CompanyAlreadyExistsException extends RuntimeException {

	public CompanyAlreadyExistsException(String message) {
		super(message);
	}
}
