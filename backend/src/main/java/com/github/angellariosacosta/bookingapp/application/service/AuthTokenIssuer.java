package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
class AuthTokenIssuer {

    private final TokenService tokenService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshExpirationMinutes;

    record IssuedTokens(AuthTokenCommand command, Long refreshTokenId) {}

    IssuedTokens issue(User user, String userAgent, String ipAddress) {
        List<String> roles = user.getRoles().stream().map(Role::name).toList();
        String accessToken = tokenService.generateToken(user.getId(), user.getEmail(), roles);

        String rawRefreshToken = refreshTokenGenerator.generateRawToken();
        Instant now = Instant.now();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshTokenGenerator.hash(rawRefreshToken))
                .issuedAt(now)
                .expiresAt(now.plus(refreshExpirationMinutes, ChronoUnit.MINUTES))
                .revoked(false)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        RefreshToken saved = refreshTokenRepositoryPort.save(refreshToken);

        AuthTokenCommand command = new AuthTokenCommand(accessToken, rawRefreshToken, user.getId(), user.getEmail(), roles);
        return new IssuedTokens(command, saved.getId());
    }
}
