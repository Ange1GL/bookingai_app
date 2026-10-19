package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.OneTimeCodeEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.OneTimeCodeMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryOneTimeCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaOneTimeCodeRepositoryAdapter implements OneTimeCodeRepositoryPort {

    private final JpaRepositoryOneTimeCode jpaRepository;
    private final OneTimeCodeMapper mapper;

    @Override
    public OneTimeCode save(OneTimeCode code) {
        if (code.getId() == null) {
            return mapper.toDomain(jpaRepository.save(mapper.toNewEntity(code)));
        }
        OneTimeCodeEntity entity = jpaRepository.findById(code.getId())
                .orElseThrow(() -> new IllegalStateException("One-time code not found: " + code.getId()));
        mapper.copyMutableState(code, entity);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<OneTimeCode> findPendingByUserIdForUpdate(Long userId) {
        return jpaRepository.findPendingForUpdate(userId).map(mapper::toDomain);
    }

    @Override
    public void consumePending(Long userId, Instant now) {
        jpaRepository.consumePending(userId, now);
    }
}
