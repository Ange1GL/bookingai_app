package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.manual;

import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeNotifierPort;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;

import lombok.extern.slf4j.Slf4j;

/**
 * Entrega manual: no envía nada; el administrador recibe el código en la respuesta y se lo da al usuario por su
 * propio chat. El código nunca se escribe en el log.
 */
@Slf4j
public class ManualOneTimeCodeNotifierAdapter implements OneTimeCodeNotifierPort {

	@Override
	public boolean sendCode(String phone, String code) {
		log.info("Entrega manual del código de acceso para {}", PhoneMask.mask(phone));
		return false;
	}
}
