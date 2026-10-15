package com.github.angellariosacosta.bookingapp.application.port.out;

/** Entrega la respuesta al cliente por WhatsApp. */
public interface CustomerReplyPort {
	void send(String phoneNumberId, String to, String text);
}
