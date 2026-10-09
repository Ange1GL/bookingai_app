package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.CustomerBlacklistMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaCustomerBlacklistJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaCustomerBlacklistRepositoryAdapter implements CustomerBlacklistRepositoryPort {

	private final JpaCustomerBlacklistJpaRepository jpaRepository;
	private final CustomerBlacklistMapper mapper;

	@Override
	public CustomerBlacklist save(CustomerBlacklist entry) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(entry)));
	}

	@Override
	public boolean existsByCustomerId(Long customerId, Long userId) {
		return jpaRepository.existsByCustomerIdAndUserId(customerId, userId);
	}

	@Override
	public Optional<CustomerBlacklist> findByCustomerId(Long customerId, Long userId) {
		return jpaRepository.findByCustomerIdAndUserId(customerId, userId).map(mapper::toDomain);
	}

	@Override
	public boolean deleteByCustomerId(Long customerId, Long userId) {
		return jpaRepository.deleteByCustomerIdAndUserId(customerId, userId) > 0;
	}

	@Override
	public Set<Long> findBlacklistedIds(Collection<Long> customerIds, Long userId) {
		if (customerIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(jpaRepository.findBlacklistedCustomerIds(userId, customerIds));
	}
}
