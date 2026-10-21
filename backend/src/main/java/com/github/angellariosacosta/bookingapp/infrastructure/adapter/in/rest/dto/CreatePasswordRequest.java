package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePasswordRequest(
		@NotBlank(message = "El usuario es obligatorio")
		@Size(max = 50, message = "El usuario no puede superar los 50 caracteres")
		String username,

		@NotBlank(message = "El código es obligatorio")
		@Pattern(regexp = "\\d{4,15}", message = "El código solo tiene dígitos")
		String code,

		@NotBlank(message = "El comprobante de verificación es obligatorio")
		@Size(max = 200, message = "El comprobante de verificación no es válido")
		String verificationToken,

		@NotBlank(message = "La nueva contraseña es obligatoria")
		@Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
		String newPassword
) {

	// Evita que un log accidental del request vuelque el código y la contraseña.
	@Override
	public String toString() {
		return "CreatePasswordRequest[username=" + username + "]";
	}
}
