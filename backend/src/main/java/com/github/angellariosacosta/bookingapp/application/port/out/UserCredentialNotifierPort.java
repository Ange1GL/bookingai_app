package com.github.angellariosacosta.bookingapp.application.port.out;

/** Entrega la contraseña temporal al usuario por WhatsApp. A diferencia de CustomerReplyPort, informa si salió. */
public interface UserCredentialNotifierPort {

	/** @return true si Meta aceptó el mensaje; false si WhatsApp no está configurado o el envío falló. */
	boolean sendTemporaryPassword(String phone, String username, String temporaryPassword);
}
