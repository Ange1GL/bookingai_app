package com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion;

public enum JwtErrorCode {
    TOKEN_EXPIRED(401, "Token expirado"),
    TOKEN_INVALID(401, "Token inválido"),
    TOKEN_MALFORMED(401, "Token mal formado"),
    TOKEN_SIGNATURE_INVALID(401, "Firma del token inválida");

    private final int httpStatus;
    private final String message;

    JwtErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
