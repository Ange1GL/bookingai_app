package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.scheduler;

import com.github.angellariosacosta.bookingapp.application.port.out.AccessTokenRevocationRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Purga las filas ya vencidas de revoked_access_token. El margen de seguridad
// (clockSkewSafetyMarginMinutes) tiene que ser mayor que la tolerancia de desfase
// de reloj del propio JwtDecoder (JwtTimestampValidator, ~60s por defecto): si se
// borrara justo al vencer, un token que el decoder todavía acepta por ese margen
// de tolerancia podría dejar de estar bloqueado antes de que realmente sea inválido.
@Slf4j
@Component
@RequiredArgsConstructor
public class RevokedAccessTokenCleanupJob {

    private final AccessTokenRevocationRepositoryPort accessTokenRevocationRepositoryPort;

    @Value("${security.jwt.revocation-cleanup.clock-skew-safety-margin-minutes}")
    private long clockSkewSafetyMarginMinutes;

    @Scheduled(fixedDelayString = "#{${security.jwt.revocation-cleanup.interval-minutes} * 60000}")
    public void purgeExpiredEntries() {
        Instant cutoff = Instant.now().minus(clockSkewSafetyMarginMinutes, ChronoUnit.MINUTES);
        accessTokenRevocationRepositoryPort.deleteExpiredBefore(cutoff);
        log.debug("Purged revoked access token entries older than {}", cutoff);
    }
}
