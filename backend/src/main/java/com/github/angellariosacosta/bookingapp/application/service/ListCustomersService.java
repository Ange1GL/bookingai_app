package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListCustomersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListCustomersService implements ListCustomersUseCase {

	private final CustomerRepositoryPort customerRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResult<Customer> list(ListCustomersQuery query) {
		return customerRepository.findPage(normalize(query));
	}

	// Un filtro en blanco equivale a no filtrar.
	private ListCustomersQuery normalize(ListCustomersQuery query) {
		return new ListCustomersQuery(
				query.userId(),
				blankToNull(query.name()),
				blankToNull(query.phone()),
				query.page(),
				query.size(),
				query.sortBy(),
				query.direction());
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
