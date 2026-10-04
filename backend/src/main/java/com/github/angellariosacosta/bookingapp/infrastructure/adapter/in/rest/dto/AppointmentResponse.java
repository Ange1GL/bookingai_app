package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

public record AppointmentResponse(
		Long id,
		LocalDateTime startTime,
		LocalDateTime endTime,
		Long customerId,
		String customerName,
		String status
) {
}
