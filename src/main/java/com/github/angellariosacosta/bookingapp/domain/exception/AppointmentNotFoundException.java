package com.github.angellariosacosta.bookingapp.domain.exception;

public class AppointmentNotFoundException extends RuntimeException {
	public AppointmentNotFoundException(String message) {
		super(message);
	}
}
