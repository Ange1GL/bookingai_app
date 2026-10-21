package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PasswordRecoveryRequestEntity;

public interface JpaRepositoryPasswordRecoveryRequest extends JpaRepository<PasswordRecoveryRequestEntity, Long> {

    // El índice único parcial (una pendiente por usuario) resuelve la carrera entre dos solicitudes simultáneas.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into password_recovery_request (user_id, requested_at)
            values (:userId, :now)
            on conflict (user_id) where resolved_at is null do nothing
            """, nativeQuery = true)
    int registerPending(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PasswordRecoveryRequestEntity r set r.resolvedAt = :now where r.userId = :userId and r.resolvedAt is null")
    int resolvePending(@Param("userId") Long userId, @Param("now") Instant now);

    @Query("""
            select r from PasswordRecoveryRequestEntity r join fetch r.user u
            where r.resolvedAt is null and u.deleted = false
            order by r.requestedAt asc
            """)
    List<PasswordRecoveryRequestEntity> findPending();
}
