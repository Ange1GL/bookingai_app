package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(
		Long customerId,
		LocalDateTime startTime,
		LocalDateTime endTime
) {
}
