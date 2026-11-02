package com.github.angellariosacosta.bookingapp.application.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.RevokeSessionByUserIdUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevokeSessionByUserIdService implements RevokeSessionByUserIdUseCase {

    private final UserFinder userFinder;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    // Solo se revocan los refresh tokens: el access token vive como maximo jwt.expiration (15 min) y despues
    // ya no se puede renovar. Igual criterio que ChangeUserStatusService.disable.
    @Override
    @Transactional
    public void revokeSessionByUserId(Long userId) {
        userFinder.getOrThrow(userId);
        refreshTokenRepositoryPort.revokeAllByUserId(userId);
    }
}
