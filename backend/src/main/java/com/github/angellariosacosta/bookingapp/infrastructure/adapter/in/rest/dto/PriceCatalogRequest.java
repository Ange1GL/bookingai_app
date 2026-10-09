package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PriceCatalogRequest(
		@NotBlank(message = "El nombre del servicio es obligatorio")
		String label,

		@NotNull(message = "El precio es obligatorio")
		@Positive(message = "El precio debe ser mayor a 0")
		Long price
) {
}
