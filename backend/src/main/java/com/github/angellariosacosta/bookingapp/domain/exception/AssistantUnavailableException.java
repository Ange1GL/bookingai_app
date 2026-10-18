package com.github.angellariosacosta.bookingapp.domain.exception;

/** El proveedor de IA del asistente falló o no respondió. */
public class AssistantUnavailableException extends RuntimeException {

	public AssistantUnavailableException(Throwable cause) {
		super(cause);
	}
}
