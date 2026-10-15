package com.github.angellariosacosta.bookingapp.application.port.in;

import java.util.Optional;

import com.github.angellariosacosta.bookingapp.domain.model.Customer;

public interface FindCustomerByPhoneUseCase {
	Optional<Customer> find(String phone, Long userId);
}
