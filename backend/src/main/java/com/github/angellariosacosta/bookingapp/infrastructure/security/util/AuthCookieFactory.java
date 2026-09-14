package com.github.angellariosacosta.bookingapp.infrastructure.security.util;

import com.github.angellariosacosta.bookingapp.infrastructure.config.CookieProperties;
import com.github.angellariosacosta.bookingapp.infrastructure.config.CookieProperties.CookieSettings;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieFactory {

    private final CookieProperties cookieProperties;

    public ResponseCookie buildAccessCookie(String accessToken) {
        return build(cookieProperties.getAccess(), accessToken);
    }

    public ResponseCookie buildRefreshCookie(String refreshToken) {
        return build(cookieProperties.getRefresh(), refreshToken);
    }

    public ResponseCookie expireAccessCookie() {
        return buildExpired(cookieProperties.getAccess());
    }

    public ResponseCookie expireRefreshCookie() {
        return buildExpired(cookieProperties.getRefresh());
    }

    private ResponseCookie build(CookieSettings settings, String value) {
        return ResponseCookie.from(settings.getName(), value)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(settings.getPath())
                .maxAge(Duration.ofSeconds(settings.getMaxAge()))
                .build();
    }

    private ResponseCookie buildExpired(CookieSettings settings) {
        return ResponseCookie.from(settings.getName(), "")
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(settings.getPath())
                .maxAge(Duration.ZERO)
                .build();
    }
}
