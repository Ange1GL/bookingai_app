package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestPasswordRecoveryRequest(
		@NotBlank(message = "El username es obligatorio")
		@Size(max = 50, message = "El username no puede superar los 50 caracteres")
		String username
) {
}
