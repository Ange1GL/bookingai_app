package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Uno a uno con el usuario dueño: {@code user_id} es la clave primaria. */
@Entity
@Table(name = "company")
@NoArgsConstructor
@Getter
@Setter
public class CompanyEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(nullable = false, length = 100)
	private String name;

	// Solo lectura (para buscar por nombre del dueño); la escritura va por userId.
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private UserEntity user;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;
}
