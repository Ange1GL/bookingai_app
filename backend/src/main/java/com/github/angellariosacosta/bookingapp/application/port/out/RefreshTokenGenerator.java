package com.github.angellariosacosta.bookingapp.application.port.out;

public interface RefreshTokenGenerator {
    String generateRawToken();

    String hash(String rawToken);
}
