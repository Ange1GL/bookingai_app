package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;

import jakarta.validation.constraints.Size;

public record BlacklistCustomerRequest(
		@Size(max = CustomerBlacklist.MAX_REASON_LENGTH, message = "El motivo no puede superar los 250 caracteres")
		String reason
) {
}
