package com.github.angellariosacosta.bookingapp.application.port.in;

public interface SendTestPushUseCase {

	/** Envía una notificación de prueba a los dispositivos del usuario; devuelve a cuántos llegó. */
	int sendTest(Long userId);
}
