package com.github.angellariosacosta.bookingapp.application.port.out;

/** Entrega el código de un solo uso al usuario por WhatsApp. A diferencia de CustomerReplyPort, informa si salió. */
public interface OneTimeCodeNotifierPort {

	/** @return true si Meta aceptó el mensaje; false si WhatsApp no está configurado o el envío falló. */
	boolean sendCode(String phone, String code);
}
