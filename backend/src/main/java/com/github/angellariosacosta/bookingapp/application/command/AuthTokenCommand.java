package com.github.angellariosacosta.bookingapp.application.command;

public record AuthTokenCommand(String token, String refreshToken) {}
