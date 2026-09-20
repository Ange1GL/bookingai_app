package com.github.angellariosacosta.bookingapp.application.port.out;

public interface TokenService {
    String generateToken(Long subject, String username);

    String getUsername(String token);

    boolean validateToken(String token);
}
