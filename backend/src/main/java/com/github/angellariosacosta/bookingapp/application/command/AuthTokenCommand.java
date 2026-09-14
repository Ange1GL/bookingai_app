package com.github.angellariosacosta.bookingapp.application.command;

import java.util.List;

public record AuthTokenCommand(
        String accessToken,
        String refreshToken,
        Long userId,
        String email,
        List<String> roles
) {}
