package com.github.angellariosacosta.bookingapp.application.command;

import java.time.LocalDateTime;

public record BookAppointmentCommand(
		String name,
		String phone,
		LocalDateTime startTime,
		LocalDateTime endTime
) {}
