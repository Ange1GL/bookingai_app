package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

public record CreateCustomerRequest(
		String name,
		String phone
) {
}
