package com.github.angellariosacosta.bookingapp.application.command;

import java.time.LocalDateTime;

public record RescheduleAppointmentCommand(Long appointmentId, LocalDateTime newStart, LocalDateTime newEnd, Long userId) {}
