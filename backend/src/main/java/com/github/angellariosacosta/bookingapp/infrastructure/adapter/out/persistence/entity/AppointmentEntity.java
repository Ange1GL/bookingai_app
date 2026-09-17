package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
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
@Table(name = "appointment")
@Getter
@NoArgsConstructor
@Setter
public class AppointmentEntity {

	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	
	@Column(name = "customer_id")
	private Long customerId;
	

	
	@ManyToOne
	@JoinColumn(
			name = "customer_id",
			referencedColumnName = "customer_id",
			updatable = false,
			insertable = false,
			foreignKey = @ForeignKey(name = "fk_appointment_customer")
	)
	private CustomerEntity customer;

	@Column(name = "status_id")
	private Integer statusId;


	@ManyToOne
	@JoinColumn(
			name = "status_id",
			referencedColumnName = "status_id",
			updatable = false,
			insertable = false,
			foreignKey = @ForeignKey(name = "fk_appointment_status")
	)
	private StatusAppointmentEntity status;

	@Column(name = "user_id")
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "user_id",
			referencedColumnName = "user_id",
			updatable = false,
			insertable = false,
			foreignKey = @ForeignKey(name = "fk_appointment_user")
	)
	private UserEntity user;


	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;
	
	
	
	
}
