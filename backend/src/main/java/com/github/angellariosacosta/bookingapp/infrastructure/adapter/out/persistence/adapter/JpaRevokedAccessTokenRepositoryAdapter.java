package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import com.github.angellariosacosta.bookingapp.application.port.out.AccessTokenRevocationRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.RevokedAccessToken;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.RevokedAccessTokenMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRevokedAccessToken;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JpaRevokedAccessTokenRepositoryAdapter implements AccessTokenRevocationRepositoryPort {

    private final JpaRepositoryRevokedAccessToken jpaRepositoryRevokedAccessToken;
    private final RevokedAccessTokenMapper revokedAccessTokenMapper;

    @Override
    public void revoke(RevokedAccessToken revokedAccessToken) {
        jpaRepositoryRevokedAccessToken.save(revokedAccessTokenMapper.toEntity(revokedAccessToken));
    }

    @Override
    public boolean existsByJti(String jti) {
        return jpaRepositoryRevokedAccessToken.existsByJti(jti);
    }

    @Override
    @Transactional
    public void deleteExpiredBefore(Instant cutoff) {
        jpaRepositoryRevokedAccessToken.deleteAllByExpiresAtBefore(cutoff);
    }
}
