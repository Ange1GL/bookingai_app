package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;

public interface RegisterUserCase {
    AuthTokenResult register(String username, String email, String rawPassword, String name, String userAgent, String ipAddress);
}
