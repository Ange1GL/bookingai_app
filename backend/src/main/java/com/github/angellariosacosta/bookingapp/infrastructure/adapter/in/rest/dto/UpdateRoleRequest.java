package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
		@NotBlank(message = "El nombre del rol es obligatorio")
		@Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
		String name,

		@Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
		String description
) {
}
