package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.application.result.DecodedToken;

public interface TokenService {
    String generateToken(Long subject, String username);

    // Lanza JwtAuthenticationException si el token es inválido, expiró o está mal formado.
    DecodedToken decode(String token);
}
