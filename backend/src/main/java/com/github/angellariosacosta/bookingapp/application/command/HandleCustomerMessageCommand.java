package com.github.angellariosacosta.bookingapp.application.command;

/** Mensaje de texto de un cliente final recibido por WhatsApp. {@code from} ya viene autenticado por la firma HMAC. */
public record HandleCustomerMessageCommand(String phoneNumberId, String from, String type, String text) {

	private static final String TEXT_TYPE = "text";

	/** Identifica la conversación (negocio de WhatsApp + cliente); es la clave del estado y de la memoria del chat. */
	public String conversationKey() {
		return phoneNumberId + ":" + from;
	}

	public boolean hasReadableText() {
		return TEXT_TYPE.equals(type) && text != null && !text.isBlank();
	}

	public boolean hasSender() {
		return phoneNumberId != null && !phoneNumberId.isBlank() && from != null && !from.isBlank();
	}
}
