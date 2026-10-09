package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.CreateCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateCustomerRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerPageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerResponse;

@Component
public class CustomerRestMapper {

	public CreateCustomerCommand toCommand(CreateCustomerRequest request, Long userId) {
		return new CreateCustomerCommand(request.name(), request.phone(), userId);
	}

	public CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone());
	}

	public CustomerPageResponse toPageResponse(PageResult<Customer> page) {
		PageResult<CustomerResponse> mapped = page.map(this::toResponse);
		return new CustomerPageResponse(
				mapped.content(), mapped.page(), mapped.size(), mapped.totalElements(), mapped.totalPages());
	}
}
