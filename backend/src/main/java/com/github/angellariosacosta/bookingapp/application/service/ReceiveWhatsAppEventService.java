package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand;
import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand.IncomingMessage;
import com.github.angellariosacosta.bookingapp.application.port.in.ReceiveWhatsAppEventUseCase;

import lombok.extern.slf4j.Slf4j;

/**
 * Fase 1: solo registra metadatos del mensaje. Nunca el texto ni el teléfono completo (datos personales).
 * Punto de enganche para el procesamiento con el agente (Fase 2).
 */
@Slf4j
@Service
public class ReceiveWhatsAppEventService implements ReceiveWhatsAppEventUseCase {

	private static final int VISIBLE_PHONE_DIGITS = 4;
	private static final String PHONE_MASK = "***";

	@Override
	public void receive(ReceiveWhatsAppEventCommand command) {
		for (IncomingMessage message : command.messages()) {
			log.info("WhatsApp mensaje recibido id={} type={} from={}",
					message.messageId(), message.type(), maskPhone(message.from()));
		}
	}

	private static String maskPhone(String phone) {
		if (phone == null || phone.length() <= VISIBLE_PHONE_DIGITS) {
			return PHONE_MASK;
		}
		return PHONE_MASK + phone.substring(phone.length() - VISIBLE_PHONE_DIGITS);
	}
}
