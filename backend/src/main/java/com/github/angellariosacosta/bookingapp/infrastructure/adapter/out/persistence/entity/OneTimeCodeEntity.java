package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "one_time_code")
@Getter
@Setter
@NoArgsConstructor
public class OneTimeCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "one_time_code_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            updatable = false,
            insertable = false,
            foreignKey = @ForeignKey(name = "fk_one_time_code_user")
    )
    private UserEntity user;

    @Column(name = "purpose_id", nullable = false)
    private Integer purposeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "purpose_id",
            referencedColumnName = "purpose_id",
            updatable = false,
            insertable = false,
            foreignKey = @ForeignKey(name = "fk_one_time_code_purpose")
    )
    private OneTimeCodePurposeEntity purpose;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
