package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.SearchCustomersUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchCustomersService implements SearchCustomersUseCase {

	private final CustomerRepository customerRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Customer> search(String name) {
		return customerRepository.searchByNameContaining(name);
	}
}
