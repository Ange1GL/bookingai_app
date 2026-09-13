package com.github.angellariosacosta.bookingapp.domain.exception;

public class InvalidAppointmentTimeRangeException extends RuntimeException {

	public InvalidAppointmentTimeRangeException(String message) {
		super(message);
	}
}
