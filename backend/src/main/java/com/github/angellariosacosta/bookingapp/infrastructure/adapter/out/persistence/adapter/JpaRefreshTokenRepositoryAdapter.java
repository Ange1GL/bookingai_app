package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.RefreshTokenMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRefreshToken;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final JpaRepositoryRefreshToken jpaRepositoryRefreshToken;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity saved = jpaRepositoryRefreshToken.save(refreshTokenMapper.toEntity(refreshToken));
        return refreshTokenMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepositoryRefreshToken.findByTokenHash(tokenHash).map(refreshTokenMapper::toDomain);
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        List<RefreshTokenEntity> tokens = jpaRepositoryRefreshToken.findAllByUserIdAndRevokedFalse(userId);
        tokens.forEach(token -> token.setRevoked(true));
        jpaRepositoryRefreshToken.saveAll(tokens);
    }
}
