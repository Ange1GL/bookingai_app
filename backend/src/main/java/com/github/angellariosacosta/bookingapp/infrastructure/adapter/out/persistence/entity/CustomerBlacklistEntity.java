package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import com.github.angellariosacosta.bookingapp.domain.model.BlacklistSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_blacklist")
@Getter
@Setter
@NoArgsConstructor
public class CustomerBlacklistEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "customer_blacklist_id")
	private Long id;

	@Column(name = "customer_id", nullable = false)
	private Long customerId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BlacklistSource source;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
