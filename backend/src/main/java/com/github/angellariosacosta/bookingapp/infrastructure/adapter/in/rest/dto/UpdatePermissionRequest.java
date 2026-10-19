package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Size;

public record UpdatePermissionRequest(
		@Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
		String description
) {
}
