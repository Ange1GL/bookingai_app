package com.github.angellariosacosta.bookingapp.application.command;

import java.util.List;

/** Mensajes entrantes ya normalizados de un evento del webhook de WhatsApp. */
public record ReceiveWhatsAppEventCommand(List<IncomingMessage> messages) {

	public record IncomingMessage(String phoneNumberId, String from, String messageId, String type, String text) {}
}
