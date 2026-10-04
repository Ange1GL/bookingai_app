package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
		@NotBlank(message = "El nombre no puede estar vacío")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String name,

		@NotBlank(message = "El teléfono no puede estar vacío")
		@Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
		String phone
) {
}
