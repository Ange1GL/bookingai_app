package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.List;
import java.util.Optional;

import com.github.angellariosacosta.bookingapp.domain.model.Customer;

public interface CustomerRepository {
	Customer save(Customer customer);
	Optional<Customer> findById(Long id, Long userId);
	Optional<Customer> findByPhone(String phone, Long userId);
	List<Customer> searchByNameContaining(String name, Long userId);
}
