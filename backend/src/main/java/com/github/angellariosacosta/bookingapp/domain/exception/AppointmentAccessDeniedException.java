package com.github.angellariosacosta.bookingapp.domain.exception;

public class AppointmentAccessDeniedException extends RuntimeException {

	public AppointmentAccessDeniedException(String message) {
		super(message);
	}
}
