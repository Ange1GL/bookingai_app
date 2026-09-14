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

    private Instant expiresAt;

    private boolean revoked;

    private Long replacedBy;

    private String userAgent;

    private String ipAddress;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
