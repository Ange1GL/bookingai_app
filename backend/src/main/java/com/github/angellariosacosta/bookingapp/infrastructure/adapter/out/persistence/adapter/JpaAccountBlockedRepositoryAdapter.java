package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AccountBlockedEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.AccountBlockedMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryAccountBlocked;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaAccountBlockedRepositoryAdapter implements AccountBlockedRepository {

    private final JpaRepositoryAccountBlocked jpaRepositoryAccountBlocked;
    private final AccountBlockedMapper accountBlockedMapper;

    @Override
    public AccountBlocked save(AccountBlocked accountBlocked) {
        AccountBlockedEntity entity = accountBlockedMapper.toEntity(accountBlocked);
        return accountBlockedMapper.toDomain(jpaRepositoryAccountBlocked.save(entity));
    }

    @Override
    public Optional<AccountBlocked> findByUserId(Long userId) {
        return jpaRepositoryAccountBlocked.findByUserId(userId).map(accountBlockedMapper::toDomain);
    }

    @Transactional
    @Override
    public void updateNumberOfAttempts(AccountBlocked accountBlocked) {
        jpaRepositoryAccountBlocked.updateNumberOfAttempts(accountBlocked.getNumberOfAttempts(), accountBlocked.getUserId());
    }

    @Transactional
    @Override
    public void updateExpiratedAt(AccountBlocked accountBlocked) {
        jpaRepositoryAccountBlocked.updateExpiratedAt(
                accountBlocked.getStartExpiratedAt(),
                accountBlocked.getEndExpiratedAt(),
                accountBlocked.getUserId()
        );
    }

    @Transactional
    @Override
    public void resetExpiredBlocks(Instant cutoff) {
        jpaRepositoryAccountBlocked.resetExpiredBlocks(cutoff);
    }

    @Transactional
    @Override
    public void resetAttempts(AccountBlocked accountBlocked) {
        jpaRepositoryAccountBlocked.resetAttempts(accountBlocked.getUserId());
    }
}
