package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.RevokeSessionByUserIdUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeSessionByUserIdService implements RevokeSessionByUserIdUseCase {

    private final UserFinder userFinder;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final UserRepository userRepository;

    // Los refresh tokens se revocan para que no pueda renovar; subir token_version invalida de inmediato los access
    // tokens ya emitidos (el filtro JWT los rechaza), sin esperar a que venzan.
    @Override
    @Transactional
    public void revokeSessionByUserId(Long actorId, Long userId) {
        userFinder.getOrThrow(userId);
        refreshTokenRepositoryPort.revokeAllByUserId(userId);
        userRepository.incrementTokenVersion(userId);
        log.warn("Sessions revoked target={} actor={}", userId, actorId);
    }
}
