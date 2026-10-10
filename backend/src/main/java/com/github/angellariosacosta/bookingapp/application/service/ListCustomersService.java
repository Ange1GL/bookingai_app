package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListCustomersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.result.CustomerListItem;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListCustomersService implements ListCustomersUseCase {

	private final CustomerRepositoryPort customerRepository;
	private final CustomerBlacklistRepositoryPort blacklistRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResult<CustomerListItem> list(ListCustomersQuery query) {
		PageResult<Customer> page = customerRepository.findPage(normalize(query));
		Set<Long> blacklistedIds = blacklistRepository.findBlacklistedIds(idsOf(page), query.userId());
		return page.map(customer -> new CustomerListItem(customer, blacklistedIds.contains(customer.getId())));
	}

	private List<Long> idsOf(PageResult<Customer> page) {
		return page.content().stream().map(Customer::getId).toList();
	}

	// Un filtro en blanco equivale a no filtrar.
	private ListCustomersQuery normalize(ListCustomersQuery query) {
		return new ListCustomersQuery(
				query.userId(),
				blankToNull(query.name()),
				blankToNull(query.phone()),
				query.blacklisted(),
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
