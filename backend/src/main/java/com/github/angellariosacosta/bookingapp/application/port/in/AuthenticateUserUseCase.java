package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;

public interface AuthenticateUserUseCase {
    AuthTokenCommand authenticate(String email, String rawPassword);
}
