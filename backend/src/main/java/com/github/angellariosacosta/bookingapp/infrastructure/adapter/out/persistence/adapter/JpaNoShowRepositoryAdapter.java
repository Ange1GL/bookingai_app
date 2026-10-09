package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.NoShowRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.NoShow;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.NoShowMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaCustomerNoShowJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaNoShowRepositoryAdapter implements NoShowRepositoryPort {

	private final JpaCustomerNoShowJpaRepository jpaRepository;
	private final NoShowMapper mapper;

	@Override
	public NoShow save(NoShow noShow) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(noShow)));
	}

	@Override
	public boolean existsByAppointmentId(Long appointmentId, Long userId) {
		return jpaRepository.existsByAppointmentIdAndUserId(appointmentId, userId);
	}

	@Override
	public long countActiveByCustomerId(Long customerId, Long userId) {
		return jpaRepository.countByCustomerIdAndUserIdAndClearedAtIsNull(customerId, userId);
	}

	@Override
	public List<NoShow> findByCustomerId(Long customerId, Long userId) {
		return jpaRepository.findByCustomerIdAndUserIdOrderByCreatedAtDesc(customerId, userId).stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public int clearByCustomerId(Long customerId, Long userId, Instant clearedAt) {
		return jpaRepository.clearActive(customerId, userId, clearedAt);
	}
}
