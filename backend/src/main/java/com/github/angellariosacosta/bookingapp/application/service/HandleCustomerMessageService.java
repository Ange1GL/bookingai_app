package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.HandleCustomerMessageUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerAssistantPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerReplyPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleCustomerMessageService implements HandleCustomerMessageUseCase {

	static final String UNSUPPORTED_MESSAGE_REPLY = "Por ahora solo puedo leer mensajes de texto. ¿Me escribes tu mensaje?";
	static final String ASSISTANT_UNAVAILABLE_REPLY = "Tuve un problema para procesar tu mensaje. Inténtalo de nuevo en unos minutos.";

	private final CustomerAssistantPort assistant;
	private final CustomerReplyPort replyPort;

	@Override
	public void handle(HandleCustomerMessageCommand command) {
		if (!command.hasSender()) {
			log.warn("Mensaje de WhatsApp sin remitente, se ignora");
			return;
		}
		String reply = command.hasReadableText() ? askAssistant(command) : UNSUPPORTED_MESSAGE_REPLY;
		replyPort.send(command.phoneNumberId(), command.from(), reply);
	}

	private String askAssistant(HandleCustomerMessageCommand command) {
		try {
			return assistant.reply(command);
		} catch (RuntimeException ex) {
			log.error("Error del asistente al responder a un cliente de WhatsApp: {}", ex.getMessage(), ex);
			return ASSISTANT_UNAVAILABLE_REPLY;
		}
	}
}
