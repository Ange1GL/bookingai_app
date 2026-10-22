package com.github.angellariosacosta.bookingapp.domain.exception;

/** La suscripción push no es válida (p. ej. el endpoint no es de un servicio push conocido). */
public class InvalidPushSubscriptionException extends RuntimeException {

	public InvalidPushSubscriptionException(String message) {
		super(message);
	}
}
