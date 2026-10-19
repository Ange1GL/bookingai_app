package com.github.angellariosacosta.bookingapp.application.port.out;

public interface TemporaryPasswordGenerator {

	/** Contraseña temporal aleatoria, fuerte y fácil de transcribir. */
	String generate();
}
