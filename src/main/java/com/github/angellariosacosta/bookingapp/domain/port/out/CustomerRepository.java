package com.github.angellariosacosta.bookingapp.domain.port.out;

import java.util.Optional;

import com.github.angellariosacosta.bookingapp.domain.model.Customer;

public interface CustomerRepository {
	Customer save(Customer customer);
	Optional<Customer> findById(Long id);
	Optional<Customer> findByPhone(String phone);
}
