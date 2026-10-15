package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import com.github.angellariosacosta.bookingapp.domain.model.Company;

public record CompanyResponse(Long id, String name) {

	public static CompanyResponse from(Company company) {
		return new CompanyResponse(company.getId(), company.getName());
	}
}
