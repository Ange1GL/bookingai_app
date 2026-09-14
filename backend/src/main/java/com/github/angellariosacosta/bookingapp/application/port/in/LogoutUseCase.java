package com.github.angellariosacosta.bookingapp.application.port.in;

public interface LogoutUseCase {
    void logout(String rawRefreshToken);
}
