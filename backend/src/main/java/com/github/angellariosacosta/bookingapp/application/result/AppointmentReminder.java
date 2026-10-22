package com.github.angellariosacosta.bookingapp.application.result;

import java.time.LocalDateTime;

/** Cita próxima a empezar, con lo justo para armar el recordatorio (no se hidrata la cita completa). */
public record AppointmentReminder(Long appointmentId, Long userId, String customerName, String serviceLabel,
                                  LocalDateTime startTime) {
}
