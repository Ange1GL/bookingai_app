package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.command.RemoveCustomerFromBlacklistCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateCustomerUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListCustomerNoShowsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListCustomersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.RemoveCustomerFromBlacklistUseCase;
import com.github.angellariosacosta.bookingapp.application.query.CustomerSortField;
import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateCustomerRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerPageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.NoShowItemResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.CustomerRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.NoShowRestMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

	private static final String DEFAULT_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "20";
	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_NAME_FILTER_LENGTH = 100;
	private static final int MAX_PHONE_FILTER_LENGTH = 20;

	private final CreateCustomerUseCase createCustomer;
	private final ListCustomersUseCase listCustomers;
	private final RemoveCustomerFromBlacklistUseCase removeCustomerFromBlacklist;
	private final ListCustomerNoShowsUseCase listCustomerNoShows;
	private final CustomerRestMapper mapper;
	private final NoShowRestMapper noShowMapper;

	// 200 y no 201: el caso de uso es idempotente por teléfono y devuelve el cliente
	// existente si ya estaba registrado, así que no siempre se crea un recurso nuevo.
	@PostMapping
	public ResponseEntity<CustomerResponse> create(
			@Valid @RequestBody CreateCustomerRequest request,
			@CurrentUserId Long userId) {
		Customer customer = createCustomer.create(mapper.toCommand(request, userId));
		return ResponseEntity.ok(mapper.toResponse(customer));
	}

	@GetMapping
	public ResponseEntity<CustomerPageResponse> list(
			@RequestParam(required = false)
			@Size(max = MAX_NAME_FILTER_LENGTH, message = "El filtro name no puede superar los 100 caracteres")
			String name,
			@RequestParam(required = false)
			@Size(max = MAX_PHONE_FILTER_LENGTH, message = "El filtro phone no puede superar los 20 caracteres")
			String phone,
			@RequestParam(required = false) Boolean blacklisted,
			@RequestParam(defaultValue = DEFAULT_PAGE)
			@Min(value = 0, message = "page debe ser mayor o igual a 0")
			int page,
			@RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
			@Min(value = 1, message = "size debe ser al menos 1")
			@Max(value = MAX_PAGE_SIZE, message = "size no puede superar 50")
			int size,
			@RequestParam(defaultValue = "NAME") CustomerSortField sortBy,
			@RequestParam(defaultValue = "ASC") SortDirection direction,
			@CurrentUserId Long userId) {
		ListCustomersQuery query = new ListCustomersQuery(
				userId, name, phone, blacklisted, page, size, sortBy, direction);
		return ResponseEntity.ok(mapper.toPageResponse(listCustomers.list(query)));
	}

	@DeleteMapping("/{id}/blacklist")
	public ResponseEntity<Void> removeFromBlacklist(@PathVariable Long id, @CurrentUserId Long userId) {
		removeCustomerFromBlacklist.remove(new RemoveCustomerFromBlacklistCommand(id, userId));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/no-shows")
	public ResponseEntity<List<NoShowItemResponse>> listNoShows(@PathVariable Long id, @CurrentUserId Long userId) {
		return ResponseEntity.ok(noShowMapper.toItemResponses(listCustomerNoShows.list(id, userId)));
	}
}
