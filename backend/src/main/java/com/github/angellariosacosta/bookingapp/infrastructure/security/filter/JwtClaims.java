package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;

/**
 * Nombres de claims custom del JWT, compartidos entre quien los escribe
 * ({@code JwtTokenService#generateToken}) y quien los lee, para que un
 * typo entre ambos lados falle en compilación en vez de en runtime.
 */
final class JwtClaims {

    static final String USERNAME = "username";

    private JwtClaims() {
    }
}
