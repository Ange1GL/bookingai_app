package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.OneTimeCodeEntity;

public interface JpaRepositoryOneTimeCode extends JpaRepository<OneTimeCodeEntity, Long> {

    // Bloqueo pesimista: dos canjes simultáneos del mismo código no pueden ambos contarlo como vigente.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OneTimeCodeEntity o where o.userId = :userId and o.consumedAt is null")
    Optional<OneTimeCodeEntity> findPendingForUpdate(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OneTimeCodeEntity o set o.consumedAt = :now where o.userId = :userId and o.consumedAt is null")
    int consumePending(@Param("userId") Long userId, @Param("now") Instant now);
}
