package com.github.angellariosacosta.bookingapp.application.command;

public record UnsubscribeFromPushCommand(Long userId, String endpoint) {
}
