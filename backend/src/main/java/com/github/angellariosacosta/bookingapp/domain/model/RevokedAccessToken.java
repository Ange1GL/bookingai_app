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
public class RevokedAccessToken {

    private Long id;

    private String jti;

    private Instant expiresAt;

    private Instant revokedAt;
}
