package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
		String name,

		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es válido")
		@Size(max = 255, message = "El email no puede superar los 255 caracteres")
		String email,

		// null = no cambiar el teléfono actual
		@Size(max = 25, message = "El teléfono no puede superar los 25 caracteres")
		String phone
) {
}
