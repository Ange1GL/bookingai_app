package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;

public interface AuthenticateUserUseCase {
    AuthTokenResult authenticate(String username, String rawPassword, String userAgent, String ipAddress);
}
