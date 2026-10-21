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

		// Opcional: null o vacío = el usuario no tiene correo.
		@Email(message = "El email no es válido")
		@Size(max = 255, message = "El email no puede superar los 255 caracteres")
		String email,

		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
		String name,
		@NotBlank(message = "Los apellidos son obligatorios")
		@Size(max = 255, message = "Los apellidos no pueden superar los 255 caracteres")
		String lastName,

		// 10 dígitos nacionales, sin prefijo de país.
		@NotBlank(message = "El teléfono es obligatorio (se usa para entregar el código de acceso por WhatsApp)")
		@Size(max = 25, message = "El teléfono no puede superar los 25 caracteres")
		String phone,

		@NotEmpty(message = "El usuario debe tener al menos un rol")
		Set<@NotNull(message = "Los ids de roles no pueden ser nulos") Long> roleIds
) {
}
