package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreateCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateCustomerUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateCustomerService implements CreateCustomerUseCase {

	private final CustomerRepository customerRepository;

	@Override
	@Transactional
	public Customer create(CreateCustomerCommand command) {
		return customerRepository.findByPhone(command.phone(), command.userId())
				.orElseGet(() -> customerRepository.save(
						Customer.builder()
								.name(command.name())
								.phone(command.phone())
								.userId(command.userId())
								.build()
				));
	}
}
