package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Resultado de crear un usuario o reiniciar su contraseña. {@code oneTimeCode} solo aparece en desarrollo y cuando
 * WhatsApp no pudo entregarlo; en cualquier otro caso se omite.
 */
public record UserOneTimeCodeResponse(
		UserResponse user,
		boolean codeSent,
		@JsonInclude(JsonInclude.Include.NON_NULL) String oneTimeCode
) {
}
