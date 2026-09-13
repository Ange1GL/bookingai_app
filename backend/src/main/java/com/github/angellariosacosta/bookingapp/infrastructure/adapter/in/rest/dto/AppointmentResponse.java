package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

public record AppointmentResponse(
		Long id,
		LocalDateTime startTime,
		LocalDateTime endTime,
		String customerName,
		String status,
		Long userId
) {
}
