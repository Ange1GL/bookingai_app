package com.github.angellariosacosta.bookingapp.application.command;

public record SubscribeToPushCommand(Long userId, String endpoint, String p256dh, String auth, String userAgent) {
}
