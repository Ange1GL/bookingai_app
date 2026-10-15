package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "company")
@NoArgsConstructor
@Getter
@Setter
public class CompanyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "company_id")
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;
}
