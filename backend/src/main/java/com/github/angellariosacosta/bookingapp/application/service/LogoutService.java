package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.port.in.LogoutUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AccessTokenRevocationRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.DecodedToken;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.domain.model.RevokedAccessToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenService tokenService;
    private final AccessTokenRevocationRepositoryPort accessTokenRevocationRepositoryPort;

    @Override
    public void logout(String rawRefreshToken, String rawAccessToken) {
        revokeRefreshToken(rawRefreshToken);
        revokeAccessToken(rawAccessToken);
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

    // Best-effort: si el access token ya venció/es inválido, o si falla el guardado,
    // el logout debe completarse igual (limpiar cookies) — nunca peor que el hueco
    // que esto viene a cerrar.
    private void revokeAccessToken(String rawAccessToken) {
        if (rawAccessToken == null || rawAccessToken.isBlank()) {
            return;
        }

        try {
            DecodedToken decoded = tokenService.decode(rawAccessToken);
            accessTokenRevocationRepositoryPort.revoke(RevokedAccessToken.builder()
                    .jti(decoded.jti())
                    .expiresAt(decoded.expiresAt())
                    .revokedAt(Instant.now())
                    .build());
        } catch (Exception ex) {
            log.warn("Access token revocation failed during logout, continuing", ex);
        }
    }
}
