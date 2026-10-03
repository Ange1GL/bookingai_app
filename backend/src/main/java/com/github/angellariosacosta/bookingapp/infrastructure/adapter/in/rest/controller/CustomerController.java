package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.CreateCustomerUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateCustomerRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.CustomerRestMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CreateCustomerUseCase createCustomer;
	private final CustomerRestMapper mapper;

	// 200 y no 201: el caso de uso es idempotente por teléfono y devuelve el cliente
	// existente si ya estaba registrado, así que no siempre se crea un recurso nuevo.
	@PostMapping
	public ResponseEntity<CustomerResponse> create(
			@Valid @RequestBody CreateCustomerRequest request,
			@CurrentUserId Long userId) {
		Customer customer = createCustomer.create(mapper.toCommand(request, userId));
		return ResponseEntity.ok(mapper.toResponse(customer));
	}
}
