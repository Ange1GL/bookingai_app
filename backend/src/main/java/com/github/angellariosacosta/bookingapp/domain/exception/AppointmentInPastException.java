package com.github.angellariosacosta.bookingapp.domain.exception;

public class AppointmentInPastException extends RuntimeException {

	public AppointmentInPastException(String message) {
		super(message);
	}
}
