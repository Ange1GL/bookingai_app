package com.github.angellariosacosta.bookingapp.domain.model;

import com.github.angellariosacosta.bookingapp.domain.exception.AccountBlockedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountBlockedTest {

    private static final Long USER_ID = 1L;

    @Test
    void doesNotBlockBeforeReachingTheAllowedAttempts() {
        AccountBlocked accountBlocked = AccountBlocked.create(USER_ID);
        accountBlocked.incrementNumberOfAttempts();

        assertEquals(2, accountBlocked.getNumberOfAttempts());
        assertNull(accountBlocked.getEndExpiratedAt());
        assertDoesNotThrow(accountBlocked::ensureNotBlocked);
    }

    @Test
    void setsTheBlockingWindowExactlyWhenTheThresholdIsReached() {
        AccountBlocked accountBlocked = AccountBlocked.create(USER_ID);
        accountBlocked.incrementNumberOfAttempts();
        accountBlocked.incrementNumberOfAttempts();

        assertEquals(3, accountBlocked.getNumberOfAttempts());
        assertNotNull(accountBlocked.getStartExpiratedAt());
        assertNotNull(accountBlocked.getEndExpiratedAt());
        assertTrue(accountBlocked.getEndExpiratedAt().isAfter(accountBlocked.getStartExpiratedAt()));
    }

    @Test
    void throwsOnceTheAllowedAttemptsAreReached() {
        AccountBlocked accountBlocked = AccountBlocked.create(USER_ID);
        accountBlocked.incrementNumberOfAttempts();
        accountBlocked.incrementNumberOfAttempts();

        assertThrows(AccountBlockedException.class, accountBlocked::ensureNotBlocked);
    }

    @Test
    void doesNotExceedTheAllowedAttemptsOrOverwriteTheBlockingWindow() {
        AccountBlocked accountBlocked = AccountBlocked.create(USER_ID);
        accountBlocked.incrementNumberOfAttempts();
        accountBlocked.incrementNumberOfAttempts();

        accountBlocked.incrementNumberOfAttempts();

        assertEquals(3, accountBlocked.getNumberOfAttempts());
    }

    @Test
    void resetAttemptsClearsTheCounterAndTheBlockingWindow() {
        AccountBlocked accountBlocked = AccountBlocked.create(USER_ID);
        accountBlocked.incrementNumberOfAttempts();
        accountBlocked.incrementNumberOfAttempts();

        accountBlocked.resetAttempts();

        assertEquals(0, accountBlocked.getNumberOfAttempts());
        assertNull(accountBlocked.getStartExpiratedAt());
        assertNull(accountBlocked.getEndExpiratedAt());
        assertDoesNotThrow(accountBlocked::ensureNotBlocked);
    }
}
