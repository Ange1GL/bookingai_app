package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;


import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AccountBlockedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface JpaRepositoryAccountBlocked
extends JpaRepository<AccountBlockedEntity, Long>
{
    Optional<AccountBlockedEntity> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE AccountBlockedEntity a SET a.numberOfAttempts = :numberOfAttempts  WHERE a.userId = :userId")
    int updateNumberOfAttempts(
            @Param("numberOfAttempts") Short numberOfAttempts,
            @Param("userId") Long userId
    );

    @Modifying
    @Query("UPDATE AccountBlockedEntity a SET a.startExpiratedAt = :startExpiratedAt, a.endExpiratedAt = :endExpiratedAt WHERE a.userId = :userId")
    int updateExpiratedAt(
            @Param("startExpiratedAt") Instant startExpiratedAt,
            @Param("endExpiratedAt") Instant endExpiratedAt,
            @Param("userId") Long userId
    );

    @Modifying
    @Query("UPDATE AccountBlockedEntity a SET a.numberOfAttempts = 0, a.startExpiratedAt = null, a.endExpiratedAt = null WHERE a.startExpiratedAt IS NOT NULL AND a.endExpiratedAt IS NOT NULL AND a.endExpiratedAt <= :cutoff")
    int resetExpiredBlocks(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("UPDATE AccountBlockedEntity a SET a.numberOfAttempts = 0, a.startExpiratedAt = null, a.endExpiratedAt = null WHERE a.userId = :userId")
    int resetAttempts(@Param("userId") Long userId);
}
