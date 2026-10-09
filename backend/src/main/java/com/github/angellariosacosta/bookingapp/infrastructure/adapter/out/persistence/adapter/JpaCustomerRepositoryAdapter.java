package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.query.CustomerSortField;
import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.CustomerMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaCustomerJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaCustomerRepositoryAdapter implements CustomerRepositoryPort {

	private static final String TIE_BREAKER_PROPERTY = "id";

	private final JpaCustomerJpaRepository jpaRepository;
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

	@Override
	public PageResult<Customer> findPage(ListCustomersQuery query) {
		Page<CustomerEntity> page = jpaRepository.findPage(
				query.userId(),
				LikePatterns.containsIgnoreCase(query.name()),
				LikePatterns.contains(query.phone()),
				PageRequest.of(query.page(), query.size(), toSort(query)));
		return new PageResult<>(
				page.getContent().stream().map(mapper::toDomain).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}

	// El id como desempate hace estable el orden entre páginas cuando el campo principal se repite.
	private Sort toSort(ListCustomersQuery query) {
		Sort.Direction direction = query.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(direction, toProperty(query.sortBy())).and(Sort.by(direction, TIE_BREAKER_PROPERTY));
	}

	private String toProperty(CustomerSortField field) {
		return switch (field) {
			case NAME -> "fullName";
			case CREATED_AT -> "createdAt";
		};
	}
}
