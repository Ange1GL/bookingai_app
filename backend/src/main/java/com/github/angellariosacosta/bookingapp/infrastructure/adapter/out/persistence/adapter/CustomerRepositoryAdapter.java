package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepository;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.CustomerMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.CustomerRepositoryJpa;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {

	private final CustomerRepositoryJpa jpaRepository;
	private final CustomerMapper mapper;

	@Override
	public Customer save(Customer customer) {
		CustomerEntity entity = mapper.toEntity(customer);
		CustomerEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Customer> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Customer> findByPhone(String phone) {
		return jpaRepository.findByPhone(phone).map(mapper::toDomain);
	}

	@Override
	public List<Customer> searchByNameContaining(String name) {
		return jpaRepository.findByFullNameContainingIgnoreCase(name)
				.stream()
				.map(mapper::toDomain)
				.toList();
	}
}
