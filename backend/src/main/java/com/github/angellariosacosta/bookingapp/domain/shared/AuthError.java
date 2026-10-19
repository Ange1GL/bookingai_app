package com.github.angellariosacosta.bookingapp.domain.shared;

public enum AuthError {
    INVALID_CREDENTIALS,
    USER_DISABLED,
    INVALID_CURRENT_PASSWORD,
    EMAIL_ALREADY_IN_USE,
    USERNAME_ALREADY_IN_USE,
    REFRESH_TOKEN_INVALID,
    REFRESH_TOKEN_EXPIRED,
    REFRESH_TOKEN_REUSED
}
