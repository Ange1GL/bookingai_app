package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.BlacklistCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.command.CreateCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.result.BlacklistResult;
import com.github.angellariosacosta.bookingapp.application.result.CustomerListItem;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.BlacklistCustomerRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.BlacklistResultResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateCustomerRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerListItemResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerPageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CustomerResponse;

@Component
public class CustomerRestMapper {

	public CreateCustomerCommand toCommand(CreateCustomerRequest request, Long userId) {
		return new CreateCustomerCommand(request.name(), request.phone(), userId);
	}

	// El cuerpo del PUT /blacklist es opcional: sin cuerpo, el motivo queda vacío.
	public BlacklistCustomerCommand toBlacklistCommand(Long customerId, BlacklistCustomerRequest request, Long userId) {
		return new BlacklistCustomerCommand(customerId, userId, request == null ? null : request.reason());
	}

	public CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone());
	}

	public CustomerPageResponse toPageResponse(PageResult<CustomerListItem> page) {
		PageResult<CustomerListItemResponse> mapped = page.map(this::toListItemResponse);
		return new CustomerPageResponse(
				mapped.content(), mapped.page(), mapped.size(), mapped.totalElements(), mapped.totalPages());
	}

	public BlacklistResultResponse toBlacklistResponse(BlacklistResult result) {
		return new BlacklistResultResponse(
				result.customerId(), result.reason(), result.source(), result.cancelledAppointments());
	}

	private CustomerListItemResponse toListItemResponse(CustomerListItem item) {
		Customer customer = item.customer();
		return new CustomerListItemResponse(customer.getId(), customer.getName(), customer.getPhone(), item.blacklisted());
	}
}
