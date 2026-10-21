package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Resultado de crear un usuario o reiniciar su contraseña. {@code oneTimeCode} aparece cuando el código no salió por
 * WhatsApp y la entrega es manual (o en dev); {@code shareMessage} es el texto listo para copiar y pegar en el chat
 * del cliente. En cualquier otro caso ambos se omiten.
 */
public record UserOneTimeCodeResponse(
		UserResponse user,
		boolean codeSent,
		@JsonInclude(JsonInclude.Include.NON_NULL) String oneTimeCode,
		@JsonInclude(JsonInclude.Include.NON_NULL) String shareMessage
) {
}
