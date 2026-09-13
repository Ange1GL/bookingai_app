package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.CreateCustomerCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateCustomerRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerResponse;

@Component
public class CustomerRestMapper {

	public CreateCustomerCommand toCommand(CreateCustomerRequest request, Long userId) {
		return new CreateCustomerCommand(request.name(), request.phone(), userId);
	}

	public CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone());
	}
}
