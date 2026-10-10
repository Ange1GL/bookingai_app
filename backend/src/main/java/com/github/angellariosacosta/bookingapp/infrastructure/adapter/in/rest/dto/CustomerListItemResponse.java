package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

public record CustomerListItemResponse(
		Long id,
		String name,
		String phone,
		boolean blacklisted
) {
}
