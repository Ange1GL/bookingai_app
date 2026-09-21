package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AccountBlockedEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountBlockedMapper {

    public AccountBlocked toDomain(AccountBlockedEntity entity) {
        return AccountBlocked.reconstitute(
                entity.getUserId(),
                entity.getCreatedAt(),
                entity.getStartExpiratedAt(),
                entity.getEndExpiratedAt(),
                entity.getNumberOfAttempts()
        );
    }

    public AccountBlockedEntity toEntity(AccountBlocked domain) {
        AccountBlockedEntity entity = new AccountBlockedEntity();
        entity.setUserId(domain.getUserId());
        entity.setNumberOfAttempts(domain.getNumberOfAttempts());
        entity.setStartExpiratedAt(domain.getStartExpiratedAt());
        entity.setEndExpiratedAt(domain.getEndExpiratedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }



}
