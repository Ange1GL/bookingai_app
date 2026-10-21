package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.port.in.LogoutUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenRevoker accessTokenRevoker;

    @Override
    public void logout(String rawRefreshToken, String rawAccessToken) {
        revokeRefreshToken(rawRefreshToken);
        accessTokenRevoker.revoke(rawAccessToken);
    }

    private void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        refreshTokenRepositoryPort.findByTokenHash(tokenHash)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepositoryPort.save(refreshToken);
                });
    }
}
