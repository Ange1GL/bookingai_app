package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

public record PriceCatalogResponse(
		Integer id,
		String label,
		Long price
) {
}
