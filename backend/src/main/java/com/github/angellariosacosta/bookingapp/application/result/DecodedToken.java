package com.github.angellariosacosta.bookingapp.application.result;

import java.time.Instant;

/** {@code tokenVersion} es 0 cuando el token no trae el claim (emitido antes de existir la versión de sesión). */
public record DecodedToken(String username, String jti, Instant expiresAt, int tokenVersion) {}
