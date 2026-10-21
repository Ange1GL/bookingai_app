package com.github.angellariosacosta.bookingapp.application.port.out;

public interface OneTimeCodeGenerator {

	/** Código numérico aleatorio, corto y fácil de teclear. */
	String generate();
}
