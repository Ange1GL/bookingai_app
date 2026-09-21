package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.scheduler;

import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

// Cada cuenta bloqueada tiene su propio endExpiratedAt (calculado al momento en que
// cruzó el umbral de intentos), no hay un unico temporizador global. Este job hace
// polling cada interval-minutes y, en cada corrida, desbloquea las cuentas cuyo
// propio endExpiratedAt ya haya pasado — igual patron que RevokedAccessTokenCleanupJob.
@Slf4j
@Component
public class ResetAccountLockedJob {

    private final AccountBlockedRepository accountBlockedRepository;

    public ResetAccountLockedJob(AccountBlockedRepository accountBlockedRepository) {
        this.accountBlockedRepository = accountBlockedRepository;
    }

    @Scheduled(fixedDelayString = "#{${security.account-lock.reset-cleanup.interval-minutes} * 60000}")
    public void resetExpiredBlocks() {
        Instant now = Instant.now();
        accountBlockedRepository.resetExpiredBlocks(now);
        log.debug("Reset account locks with endExpiratedAt before {}", now);
    }
}
