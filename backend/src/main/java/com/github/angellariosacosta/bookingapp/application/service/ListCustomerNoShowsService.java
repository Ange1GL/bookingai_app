package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListCustomerNoShowsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.NoShowRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.NoShow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListCustomerNoShowsService implements ListCustomerNoShowsUseCase {

	private final CustomerRepositoryPort customerRepository;
	private final NoShowRepositoryPort noShowRepository;

	@Override
	@Transactional(readOnly = true)
	public List<NoShow> list(Long customerId, Long userId) {
		customerRepository.findById(customerId, userId)
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
		return noShowRepository.findByCustomerId(customerId, userId);
	}
}
