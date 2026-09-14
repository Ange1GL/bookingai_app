package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllByUserId(Long userId);
}
