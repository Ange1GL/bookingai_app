package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.PasswordRecoveryRequestRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.PasswordRecoveryRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.UserMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryPasswordRecoveryRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaPasswordRecoveryRequestRepositoryAdapter implements PasswordRecoveryRequestRepositoryPort {

    private final JpaRepositoryPasswordRecoveryRequest jpaRepository;
    private final UserMapper userMapper;

    @Override
    public void registerPending(Long userId, Instant now) {
        jpaRepository.registerPending(userId, now);
    }

    @Override
    public void resolvePending(Long userId, Instant now) {
        jpaRepository.resolvePending(userId, now);
    }

    @Override
    public List<PasswordRecoveryRequest> findPending() {
        return jpaRepository.findPending().stream()
                .map(entity -> new PasswordRecoveryRequest(
                        entity.getId(), userMapper.toDomain(entity.getUser()), entity.getRequestedAt()))
                .toList();
    }
}
