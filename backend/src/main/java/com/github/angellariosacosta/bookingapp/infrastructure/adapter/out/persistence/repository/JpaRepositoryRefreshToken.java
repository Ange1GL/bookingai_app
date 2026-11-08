package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JpaRepositoryRefreshToken extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findAllByUserIdAndRevokedFalse(Long userId);

    List<RefreshTokenEntity> findAllByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIssuedAtDesc(Long userId, Instant now);

    Optional<RefreshTokenEntity> findByIdAndUserId(Long id, Long userId);
}
