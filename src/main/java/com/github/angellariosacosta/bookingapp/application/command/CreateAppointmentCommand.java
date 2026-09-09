package com.github.angellariosacosta.bookingapp.application.command;

import java.time.LocalDateTime;

public record CreateAppointmentCommand(
		LocalDateTime startTime,
		LocalDateTime endTime,
		Long customerId
) {
}
