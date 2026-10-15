package com.github.angellariosacosta.bookingapp.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.FindCustomerByPhoneUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindCustomerByPhoneService implements FindCustomerByPhoneUseCase {

	private final CustomerRepositoryPort customerRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<Customer> find(String phone, Long userId) {
		return customerRepository.findByPhone(phone, userId);
	}
}
