package com.github.angellariosacosta.bookingapp.application.port.in;

public interface GetPushPublicKeyUseCase {

	/**
	 * @throws com.github.angellariosacosta.bookingapp.domain.exception.PushNotConfiguredException si faltan las claves VAPID
	 */
	String getPublicKey();
}
