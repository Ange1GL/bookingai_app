package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

public record BookAppointmentRequest(
		String name,
		String phone,
		LocalDateTime startTime,
		LocalDateTime endTime
) {}
