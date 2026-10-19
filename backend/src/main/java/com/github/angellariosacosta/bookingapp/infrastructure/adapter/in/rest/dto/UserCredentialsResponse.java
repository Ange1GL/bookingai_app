package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Resultado de crear un usuario o reiniciar su contraseña. {@code temporaryPassword} solo aparece en desarrollo y
 * cuando WhatsApp no pudo entregarla; en cualquier otro caso se omite.
 */
public record UserCredentialsResponse(
		UserResponse user,
		boolean credentialsSent,
		@JsonInclude(JsonInclude.Include.NON_NULL) String temporaryPassword
) {
}
