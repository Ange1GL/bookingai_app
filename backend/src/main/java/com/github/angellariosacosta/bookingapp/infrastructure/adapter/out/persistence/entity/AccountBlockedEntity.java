package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "account_blocked")
@Getter
@NoArgsConstructor
@Setter
public class AccountBlockedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_blocked_od_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "number_of_attempts")
    Short numberOfAttempts;

    @Column(name = "start_expirated_at")
    Instant startExpiratedAt;

    @Column(name = "end_expirated_at")
    Instant endExpiratedAt;

    @Column(name = "created_at")
    @CreationTimestamp
    Instant createdAt;

    @OneToOne
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            updatable = false,
            insertable = false,
            foreignKey = @ForeignKey(name = "fk_account_blocked_user")

    )
    private UserEntity user;

}
