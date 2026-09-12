package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.CreateCustomerUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateCustomerRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.CustomerRestMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CreateCustomerUseCase createCustomerUseCase;
	private final CustomerRestMapper mapper;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CustomerResponse create(@RequestBody CreateCustomerRequest request) {
		Customer customer = createCustomerUseCase.create(mapper.toCommand(request));
		return mapper.toResponse(customer);
	}
}
