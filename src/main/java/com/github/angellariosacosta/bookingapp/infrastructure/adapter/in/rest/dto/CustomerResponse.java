package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

public record CustomerResponse(
		Long id,
		String name,
		String phone
) {
}
