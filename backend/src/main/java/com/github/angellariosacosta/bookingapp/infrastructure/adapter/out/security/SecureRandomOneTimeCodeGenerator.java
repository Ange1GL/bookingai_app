package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.security;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeGenerator;

/** Código numérico de {@value #LENGTH} dígitos (cabe en una plantilla de autenticación de WhatsApp, máx. 15). */
@Component
public class SecureRandomOneTimeCodeGenerator implements OneTimeCodeGenerator {

	static final int LENGTH = 6;
	private static final int BOUND = (int) Math.pow(10, LENGTH);

	private final SecureRandom random = new SecureRandom();

	@Override
	public String generate() {
		return String.format("%0" + LENGTH + "d", random.nextInt(BOUND));
	}
}
