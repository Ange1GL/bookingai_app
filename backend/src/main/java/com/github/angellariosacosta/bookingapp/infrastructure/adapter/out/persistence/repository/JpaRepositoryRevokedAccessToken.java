package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RevokedAccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface JpaRepositoryRevokedAccessToken extends JpaRepository<RevokedAccessTokenEntity, Long> {
    boolean existsByJti(String jti);

    void deleteAllByExpiresAtBefore(Instant cutoff);
}
