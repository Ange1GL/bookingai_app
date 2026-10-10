package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.SearchCustomersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.CustomerListItem;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchCustomersService implements SearchCustomersUseCase {

	private final CustomerRepositoryPort customerRepository;
	private final CustomerBlacklistRepositoryPort blacklistRepository;

	@Override
	@Transactional(readOnly = true)
	public List<CustomerListItem> search(String name, Long userId) {
		List<Customer> customers = customerRepository.searchByNameContaining(name, userId);
		Set<Long> blacklistedIds = blacklistRepository.findBlacklistedIds(
				customers.stream().map(Customer::getId).toList(), userId);
		return customers.stream()
				.map(customer -> new CustomerListItem(customer, blacklistedIds.contains(customer.getId())))
				.toList();
	}
}
