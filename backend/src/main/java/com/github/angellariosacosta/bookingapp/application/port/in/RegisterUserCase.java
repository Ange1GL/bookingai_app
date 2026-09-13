package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;

public interface RegisterUserCase {
    AuthTokenCommand register(String email, String rawPassword, String name);
}
