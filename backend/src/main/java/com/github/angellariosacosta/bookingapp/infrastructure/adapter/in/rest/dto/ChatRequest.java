package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
		@NotBlank(message = "La sesión es obligatoria")
		@Size(max = 64, message = "La sesión no es válida")
		String sessionId,

		@NotBlank(message = "El mensaje es obligatorio")
		@Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
		String message
) {}
