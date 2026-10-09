package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerNoShowEntity;

public interface JpaCustomerNoShowJpaRepository extends JpaRepository<CustomerNoShowEntity, Long> {

	boolean existsByAppointmentIdAndUserId(Long appointmentId, Long userId);

	long countByCustomerIdAndUserIdAndClearedAtIsNull(Long customerId, Long userId);

	List<CustomerNoShowEntity> findByCustomerIdAndUserIdOrderByCreatedAtDesc(Long customerId, Long userId);

	@Modifying
	@Query("""
			update CustomerNoShowEntity n set n.clearedAt = :clearedAt
			where n.customerId = :customerId and n.userId = :userId and n.clearedAt is null
			""")
	int clearActive(
			@Param("customerId") Long customerId,
			@Param("userId") Long userId,
			@Param("clearedAt") Instant clearedAt);
}
