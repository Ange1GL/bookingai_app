package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;

/** Agente de IA que conversa con el cliente final y devuelve el texto de respuesta. */
public interface CustomerAssistantPort {
	String reply(HandleCustomerMessageCommand message);
}
