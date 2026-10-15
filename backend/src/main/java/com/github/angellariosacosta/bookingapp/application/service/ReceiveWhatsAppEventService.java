package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;
import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand;
import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand.IncomingMessage;
import com.github.angellariosacosta.bookingapp.application.port.in.HandleCustomerMessageUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ReceiveWhatsAppEventUseCase;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra metadatos del mensaje (nunca el texto ni el teléfono completo: datos personales) y lo entrega al agente
 * de clientes. Un fallo con un mensaje no debe impedir procesar los demás ni hacer que Meta reintente el evento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveWhatsAppEventService implements ReceiveWhatsAppEventUseCase {

	private final HandleCustomerMessageUseCase handleCustomerMessage;

	@Override
	public void receive(ReceiveWhatsAppEventCommand command) {
		for (IncomingMessage message : command.messages()) {
			log.info("WhatsApp mensaje recibido id={} type={} from={}",
					message.messageId(), message.type(), PhoneMask.mask(message.from()));
			dispatch(message);
		}
	}

	private void dispatch(IncomingMessage message) {
		try {
			handleCustomerMessage.handle(new HandleCustomerMessageCommand(
					message.phoneNumberId(), message.from(), message.messageId(), message.type(), message.text()));
		} catch (RuntimeException ex) {
			log.error("Error procesando mensaje de WhatsApp id={}: {}", message.messageId(), ex.getMessage(), ex);
		}
	}
}
