package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record CreateAppointmentRequest(
		@NotNull(message = "El cliente es obligatorio")
		Long customerId,

		@NotNull(message = "La hora de inicio es obligatoria")
		LocalDateTime startTime,

		@NotNull(message = "La hora de fin es obligatoria")
		LocalDateTime endTime
) {
}
