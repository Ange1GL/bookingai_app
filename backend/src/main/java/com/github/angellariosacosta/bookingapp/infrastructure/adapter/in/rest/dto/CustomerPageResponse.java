package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record CustomerPageResponse(
		List<CustomerResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
