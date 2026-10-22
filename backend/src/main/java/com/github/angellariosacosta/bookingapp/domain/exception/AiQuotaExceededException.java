package com.github.angellariosacosta.bookingapp.domain.exception;

/** Se agotaron los créditos de AI (diarios del usuario o tope global diario). */
public class AiQuotaExceededException extends RuntimeException {

	public AiQuotaExceededException(String message) {
		super(message);
	}
}
