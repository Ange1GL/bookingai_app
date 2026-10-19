package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import com.github.angellariosacosta.bookingapp.application.port.out.UserCredentialNotifierPort;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;

import lombok.extern.slf4j.Slf4j;

/**
 * Modo sin configurar (falta token, número emisor o plantilla): no envía nada. Registra solo que no se entregó; la
 * contraseña nunca se escribe en el log.
 */
@Slf4j
public class LoggingUserCredentialNotifierAdapter implements UserCredentialNotifierPort {

	@Override
	public boolean sendTemporaryPassword(String phone, String username, String temporaryPassword) {
		log.warn("WhatsApp de credenciales no configurado: no se envió la contraseña temporal de '{}' a {}", username,
				PhoneMask.mask(phone));
		return false;
	}
}
