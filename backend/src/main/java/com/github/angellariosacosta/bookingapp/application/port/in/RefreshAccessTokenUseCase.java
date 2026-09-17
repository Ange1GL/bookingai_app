package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;

public interface RefreshAccessTokenUseCase {
    AuthTokenResult refresh(String rawRefreshToken, String userAgent, String ipAddress);
}
