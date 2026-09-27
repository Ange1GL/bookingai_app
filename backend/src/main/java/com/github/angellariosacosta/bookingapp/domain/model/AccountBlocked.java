package com.github.angellariosacosta.bookingapp.domain.model;

import com.github.angellariosacosta.bookingapp.domain.exception.AccountBlockedException;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Getter
public class AccountBlocked {

    private Long userId;
    private Instant createdAt;
    private Instant startExpiratedAt;
    private Instant endExpiratedAt;
    private short numberOfAttempts;
    private static final short NUMBER_OF_ATTEMPTS_ALLOWED = 3;
    private static final long BLOCKING_TIME = 30;

    public void incrementNumberOfAttempts() {
        if (numberOfAttempts < NUMBER_OF_ATTEMPTS_ALLOWED) {
            this.numberOfAttempts++;
            if (numberOfAttempts == NUMBER_OF_ATTEMPTS_ALLOWED) {
                addBlockingTime();
            }
        }
    }

    public static AccountBlocked create(Long userId) {
        return new AccountBlocked(userId);
    }

    private AccountBlocked(Long userId) {
        this.userId = userId;
        this.createdAt = Instant.now();
        this.startExpiratedAt = null;
        this.endExpiratedAt = null;
        incrementNumberOfAttempts();
    }

    private AccountBlocked() {
    }

    // Restaura el estado tal cual viene de la base de datos, sin volver a
    // ejecutar incrementNumberOfAttempts() como hace el constructor de negocio.
    public static AccountBlocked reconstitute(Long userId, Instant createdAt, Instant startExpiratedAt,
                                               Instant endExpiratedAt, short numberOfAttempts) {
        AccountBlocked accountBlocked = new AccountBlocked();
        accountBlocked.userId = userId;
        accountBlocked.createdAt = createdAt;
        accountBlocked.startExpiratedAt = startExpiratedAt;
        accountBlocked.endExpiratedAt = endExpiratedAt;
        accountBlocked.numberOfAttempts = numberOfAttempts;
        return accountBlocked;
    }

    public void ensureNotBlocked() {
        if (numberOfAttempts >= NUMBER_OF_ATTEMPTS_ALLOWED) {
            throw new AccountBlockedException("Cuenta bloqueada por demasiados intentos fallidos. Intente nuevamente en " + BLOCKING_TIME + " minutos.");
        }
    }

    private void addBlockingTime() {
        this.startExpiratedAt = Instant.now();
        this.endExpiratedAt = startExpiratedAt.plus(BLOCKING_TIME, ChronoUnit.MINUTES);
    }

    public void resetAttempts() {
        this.numberOfAttempts = 0;
        this.startExpiratedAt = null;
        this.endExpiratedAt = null;
    }
}
