package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequest(
		@NotBlank(message = "El nombre del permiso es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String name,

		@Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
		String description
) {
}
