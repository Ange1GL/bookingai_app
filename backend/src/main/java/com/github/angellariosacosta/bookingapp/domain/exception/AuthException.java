package com.github.angellariosacosta.bookingapp.domain.exception;

import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {

    private final AuthError error;

    public AuthException(AuthError error) {
        super(error.name());
        this.error = error;
    }

}
