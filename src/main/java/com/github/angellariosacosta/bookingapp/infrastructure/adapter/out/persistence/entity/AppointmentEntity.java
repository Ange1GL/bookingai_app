package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
	

	
	@OneToOne
	@JoinColumn(
			name = "customer_id",
			referencedColumnName = "customer_id",
			updatable = false,
			insertable = false
	)
	private CustomerEntity customer;

	@Column(name = "status_id")
	private Integer statusId;
	
	
	@OneToOne
	@JoinColumn(
			name = "status_id",
			referencedColumnName = "status_id",
			updatable = false,
			insertable = false
	)
	private StatusAppointmentEntity status;
	
	
	@Column(name="created_at", updatable = false, insertable = false)
	@CreationTimestamp
	private Instant createdAt;
	
	
	
	
}
