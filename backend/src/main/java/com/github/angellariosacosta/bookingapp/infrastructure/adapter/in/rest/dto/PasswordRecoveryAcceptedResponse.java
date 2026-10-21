package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

/** Respuesta uniforme: existan o no el usuario y la solicitud, para no revelar qué cuentas existen. */
public record PasswordRecoveryAcceptedResponse(String message) {

	public static final String MESSAGE =
			"Si el usuario existe, el administrador recibirá tu solicitud y se pondrá en contacto contigo";

	public static PasswordRecoveryAcceptedResponse standard() {
		return new PasswordRecoveryAcceptedResponse(MESSAGE);
	}
}
