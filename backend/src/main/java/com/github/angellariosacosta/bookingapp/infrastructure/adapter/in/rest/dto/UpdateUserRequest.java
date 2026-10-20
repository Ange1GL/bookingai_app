package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
		String name,
		@NotBlank(message = "Los apellidos son obligatorios")
		@Size(max = 255, message = "Los apellidos no pueden superar los 255 caracteres")
		String lastName,

		// Opcional: null o vacío borra el correo.
		@Email(message = "El email no es válido")
		@Size(max = 255, message = "El email no puede superar los 255 caracteres")
		String email,

		// null = no cambiar el teléfono actual
		@Size(max = 25, message = "El teléfono no puede superar los 25 caracteres")
		String phone
) {
}
