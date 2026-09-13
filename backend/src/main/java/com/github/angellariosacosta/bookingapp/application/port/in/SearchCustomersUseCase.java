package com.github.angellariosacosta.bookingapp.application.port.in;

import java.util.List;

import com.github.angellariosacosta.bookingapp.domain.model.Customer;

public interface SearchCustomersUseCase {
	List<Customer> search(String name, Long userId);
}
