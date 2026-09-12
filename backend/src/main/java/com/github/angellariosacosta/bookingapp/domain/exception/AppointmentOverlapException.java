package com.github.angellariosacosta.bookingapp.domain.exception;

public class AppointmentOverlapException extends RuntimeException
{
	
	public AppointmentOverlapException(
			String message
			) {
		super(message);
	}

}
