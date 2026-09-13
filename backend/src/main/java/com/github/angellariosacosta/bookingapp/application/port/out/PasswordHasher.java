package com.github.angellariosacosta.bookingapp.application.port.out;

public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
