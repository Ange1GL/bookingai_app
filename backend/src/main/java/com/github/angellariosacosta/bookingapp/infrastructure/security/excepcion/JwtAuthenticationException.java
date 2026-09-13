package com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion;

import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationException extends AuthenticationException {

    private final JwtErrorCode errorCode;

    public JwtAuthenticationException(JwtErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public JwtErrorCode getErrorCode() {
        return errorCode;
    }
}
