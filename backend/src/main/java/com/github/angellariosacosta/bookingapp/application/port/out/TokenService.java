package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.Instant;

public interface TokenService {
    String generateToken(Long subject, String username);

    // Lanza JwtAuthenticationException si el token es inválido, expiró o está mal formado.
    DecodedToken decode(String token);

    record DecodedToken(String username, String jti, Instant expiresAt) {}
}
