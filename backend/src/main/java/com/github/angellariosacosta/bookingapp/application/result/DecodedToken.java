package com.github.angellariosacosta.bookingapp.application.result;

import java.time.Instant;

public record DecodedToken(String username, String jti, Instant expiresAt) {}
