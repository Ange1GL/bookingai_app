package com.github.angellariosacosta.bookingapp.application.result;

import java.util.List;

public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        String email,
        List<String> roles
) {}
