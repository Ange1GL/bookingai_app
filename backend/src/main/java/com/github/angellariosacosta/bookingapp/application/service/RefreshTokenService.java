package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.RefreshAccessTokenUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshAccessTokenUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final UserRepository userRepository;
    private final AuthTokenIssuer authTokenIssuer;

    @Override
    @Transactional
    public AuthTokenCommand refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepositoryPort.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException(AuthError.REFRESH_TOKEN_INVALID));

        if (stored.isRevoked()) {
            log.warn("Refresh token reuse detected userId={}", stored.getUserId());
            refreshTokenRepositoryPort.revokeAllByUserId(stored.getUserId());
            throw new AuthException(AuthError.REFRESH_TOKEN_REUSED);
        }

        if (stored.isExpired()) {
            throw new AuthException(AuthError.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AuthException(AuthError.REFRESH_TOKEN_INVALID));

        AuthTokenIssuer.IssuedTokens issued = authTokenIssuer.issue(user, userAgent, ipAddress);

        stored.setRevoked(true);
        stored.setReplacedBy(issued.refreshTokenId());
        refreshTokenRepositoryPort.save(stored);

        return issued.command();
    }
}
