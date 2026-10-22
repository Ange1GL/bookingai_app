package com.github.angellariosacosta.bookingapp.application.command;

/** Paso 1 de crear la contraseña: el usuario demuestra que conoce su código de un solo uso. */
public record VerifyCodeCommand(String username, String code) {

	@Override
	public String toString() {
		return "VerifyCodeCommand[username=" + username + "]";
	}
}
