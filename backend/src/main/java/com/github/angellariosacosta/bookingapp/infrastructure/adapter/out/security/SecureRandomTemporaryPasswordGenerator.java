package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.security;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.TemporaryPasswordGenerator;

/** Contraseña temporal de 12 caracteres con mayúscula, minúscula, dígito y símbolo, sin caracteres ambiguos (0/O, 1/l/I). */
@Component
public class SecureRandomTemporaryPasswordGenerator implements TemporaryPasswordGenerator {

	static final int LENGTH = 12;
	static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
	static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
	static final String DIGITS = "23456789";
	static final String SYMBOLS = "!@#$%&*?";
	private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

	private final SecureRandom random = new SecureRandom();

	@Override
	public String generate() {
		List<Character> chars = new ArrayList<>(LENGTH);
		chars.add(pick(UPPER));
		chars.add(pick(LOWER));
		chars.add(pick(DIGITS));
		chars.add(pick(SYMBOLS));
		while (chars.size() < LENGTH) {
			chars.add(pick(ALL));
		}
		Collections.shuffle(chars, random);

		StringBuilder password = new StringBuilder(LENGTH);
		chars.forEach(password::append);
		return password.toString();
	}

	private char pick(String alphabet) {
		return alphabet.charAt(random.nextInt(alphabet.length()));
	}
}
