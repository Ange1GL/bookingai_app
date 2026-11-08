package com.github.angellariosacosta.bookingapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    private Long id;

    private Long userId;

    private String tokenHash;

    private Instant issuedAt;

    /** Cuándo se abrió la sesión; se hereda en cada rotación del refresh token (issuedAt sí se reinicia). */
    private Instant sessionStartedAt;

    private Instant expiresAt;

    private boolean revoked;

    private Long replacedBy;

    private String userAgent;

    private String ipAddress;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }
}
