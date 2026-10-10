package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import com.github.angellariosacosta.bookingapp.domain.model.NoShow;

import jakarta.validation.constraints.Size;

public record RegisterNoShowRequest(
		@Size(max = NoShow.MAX_REASON_LENGTH, message = "El motivo no puede superar los 250 caracteres")
		String reason
) {
}
