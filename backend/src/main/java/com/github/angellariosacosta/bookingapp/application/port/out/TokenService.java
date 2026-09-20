package com.github.angellariosacosta.bookingapp.application.port.out;

public interface TokenService {
    String generateToken(Long subject, String username);

    // Lanza JwtAuthenticationException si el token es inválido, expiró o está mal formado.
    DecodedToken decode(String token);
}
