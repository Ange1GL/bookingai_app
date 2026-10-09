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
@Table(name = "price_catalog")
@NoArgsConstructor
@Getter
@Setter
public class PriceCatalogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "price_catalog_id")
	private Integer id;

	@Column(nullable = false)
	private String label;

	@Column(nullable = false)
	private Long price;

	@Column(name = "user_id")
	private Long userId;

	@Column(nullable = false)
	private boolean active = true;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;
}
