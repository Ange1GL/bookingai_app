package com.github.angellariosacosta.bookingapp.domain.exception;

public class AccountBlockedException  extends RuntimeException{

    public AccountBlockedException(
            String message
    ) {
        super(message);
    }
}
