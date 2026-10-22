package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyCodeRequest(
		@NotBlank(message = "El usuario es obligatorio")
		@Size(max = 50, message = "El usuario no puede superar los 50 caracteres")
		String username,

		@NotBlank(message = "El código es obligatorio")
		@Pattern(regexp = "\\d{4,15}", message = "El código solo tiene dígitos")
		String code
) {

	// Evita que un log accidental del request vuelque el código.
	@Override
	public String toString() {
		return "VerifyCodeRequest[username=" + username + "]";
	}
}
