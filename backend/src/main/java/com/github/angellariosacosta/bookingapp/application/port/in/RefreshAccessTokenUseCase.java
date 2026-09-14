package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;

public interface RefreshAccessTokenUseCase {
    AuthTokenCommand refresh(String rawRefreshToken, String userAgent, String ipAddress);
}
