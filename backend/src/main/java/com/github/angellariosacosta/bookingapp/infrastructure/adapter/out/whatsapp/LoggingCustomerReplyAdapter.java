package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerReplyPort;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;

import lombok.extern.slf4j.Slf4j;

/**
 * Modo demo (sin token de Meta): no envía nada a WhatsApp, solo muestra en el log lo que se le respondería al cliente.
 * Lo instancia {@code WhatsAppReplyConfig} cuando {@code SYSTEM_USER_EMPLOYEE_META} está vacío.
 */
@Slf4j
public class LoggingCustomerReplyAdapter implements CustomerReplyPort {

	@Override
	public void send(String phoneNumberId, String to, String text) {
		log.info("WhatsApp respuesta (demo, sin envío) to={}: {}", PhoneMask.mask(to), text);
	}
}
