package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.Instant;

public record DecodedToken(String username, String jti, Instant expiresAt) {}
