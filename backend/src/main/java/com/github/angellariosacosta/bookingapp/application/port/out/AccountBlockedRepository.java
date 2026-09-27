package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;

import java.time.Instant;
import java.util.Optional;

public interface AccountBlockedRepository {
    AccountBlocked save(AccountBlocked accountBlocked);
    Optional<AccountBlocked> findByUserId(Long userId);
    Optional<AccountBlocked> findByUserIdForUpdate(Long userId);

    void updateNumberOfAttempts(AccountBlocked accountBlocked);
    void updateExpiratedAt(AccountBlocked accountBlocked);
    void resetExpiredBlocks(Instant cutoff);
    void resetAttempts(AccountBlocked accountBlocked);
}
