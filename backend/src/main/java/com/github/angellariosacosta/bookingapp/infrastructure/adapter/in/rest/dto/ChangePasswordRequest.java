package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank(message = "La contraseña actual es obligatoria")
		String currentPassword,

		@NotBlank(message = "La nueva contraseña es obligatoria")
		@Size(max = 72, message = "La nueva contraseña no puede superar los 72 caracteres")
		String newPassword
) {

	// Evita que un log accidental del request vuelque las contraseñas.
	@Override
	public String toString() {
		return "ChangePasswordRequest[***]";
	}
}
