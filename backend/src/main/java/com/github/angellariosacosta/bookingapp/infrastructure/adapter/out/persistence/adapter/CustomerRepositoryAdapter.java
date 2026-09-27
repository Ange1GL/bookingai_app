package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.CustomerMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepositoryPort {

	private final CustomerJpaRepository jpaRepository;
	private final CustomerMapper mapper;

	@Override
	public Customer save(Customer customer) {
		CustomerEntity entity = mapper.toEntity(customer);
		CustomerEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Customer> findById(Long id, Long userId) {
		return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
	}

	@Override
	public Optional<Customer> findByPhone(String phone, Long userId) {
		return jpaRepository.findByPhoneAndUserId(phone, userId).map(mapper::toDomain);
	}

	@Override
	public List<Customer> searchByNameContaining(String name, Long userId) {
		return jpaRepository.findByFullNameContainingIgnoreCaseAndUserId(name, userId)
				.stream()
				.map(mapper::toDomain)
				.toList();
	}
}
