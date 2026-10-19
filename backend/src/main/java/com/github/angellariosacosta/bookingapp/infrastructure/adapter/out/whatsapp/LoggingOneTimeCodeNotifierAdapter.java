package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeNotifierPort;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;

import lombok.extern.slf4j.Slf4j;

/**
 * Modo sin configurar (falta token, número emisor o plantilla): no envía nada. Registra solo que no se entregó; el
 * código nunca se escribe en el log.
 */
@Slf4j
public class LoggingOneTimeCodeNotifierAdapter implements OneTimeCodeNotifierPort {

	@Override
	public boolean sendCode(String phone, String code) {
		log.warn("WhatsApp de códigos no configurado: no se envió el código a {}", PhoneMask.mask(phone));
		return false;
	}
}
