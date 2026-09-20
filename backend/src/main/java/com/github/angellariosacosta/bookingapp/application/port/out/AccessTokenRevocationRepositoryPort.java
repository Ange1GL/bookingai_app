package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.domain.model.RevokedAccessToken;

import java.time.Instant;

public interface AccessTokenRevocationRepositoryPort {
    void revoke(RevokedAccessToken revokedAccessToken);

    boolean existsByJti(String jti);

    void deleteExpiredBefore(Instant cutoff);
}
