package com.github.angellariosacosta.bookingapp.application.command;

public record RegisterNoShowCommand(Long appointmentId, Long userId, String reason) {
}
