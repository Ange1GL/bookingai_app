package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
		@NotBlank(message = "El nombre de la empresa es obligatorio")
		@Size(max = 100, message = "El nombre de la empresa no puede superar 100 caracteres")
		String name
) {
}
