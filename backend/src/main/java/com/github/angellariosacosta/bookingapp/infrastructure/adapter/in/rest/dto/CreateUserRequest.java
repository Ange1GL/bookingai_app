package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
		@NotBlank(message = "El username es obligatorio")
		@Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
		String username,

		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es válido")
		@Size(max = 255, message = "El email no puede superar los 255 caracteres")
		String email,

		@Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
		String name,

		@NotBlank(message = "El teléfono es obligatorio (se usa para enviar la contraseña por WhatsApp)")
		@Size(max = 25, message = "El teléfono no puede superar los 25 caracteres")
		String phone,

		@NotEmpty(message = "El usuario debe tener al menos un rol")
		Set<@NotNull(message = "Los ids de roles no pueden ser nulos") Long> roleIds
) {
}
