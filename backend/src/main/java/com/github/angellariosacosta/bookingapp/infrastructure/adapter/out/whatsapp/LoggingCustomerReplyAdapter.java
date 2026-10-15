package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerReplyPort;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;

import lombok.extern.slf4j.Slf4j;

/** Demo: no envía nada a WhatsApp, solo muestra en el log lo que se le respondería al cliente. */
@Slf4j
@Component
public class LoggingCustomerReplyAdapter implements CustomerReplyPort {

	@Override
	public void send(String phoneNumberId, String to, String text) {
		log.info("WhatsApp respuesta (demo, sin envío) to={}: {}", PhoneMask.mask(to), text);
	}
}
