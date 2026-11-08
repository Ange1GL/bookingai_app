package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllByUserId(Long userId);

    /** Sesiones vivas del usuario: refresh tokens sin revocar y no vencidos a {@code now}, la más reciente primero. */
    List<RefreshToken> findActiveByUserId(Long userId, Instant now);

    Optional<RefreshToken> findByIdAndUserId(Long id, Long userId);
}
