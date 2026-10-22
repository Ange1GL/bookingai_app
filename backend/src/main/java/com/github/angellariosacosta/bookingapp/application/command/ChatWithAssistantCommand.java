package com.github.angellariosacosta.bookingapp.application.command;

public record ChatWithAssistantCommand(Long userId, String sessionId, String message) {
}
