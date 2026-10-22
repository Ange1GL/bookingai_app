package com.github.angellariosacosta.bookingapp.domain.exception;

/** Las notificaciones push no están configuradas en este entorno (faltan las claves VAPID). */
public class PushNotConfiguredException extends RuntimeException {

	public PushNotConfiguredException(String message) {
		super(message);
	}
}
