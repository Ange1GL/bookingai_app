package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import com.github.angellariosacosta.bookingapp.domain.model.Company;

public record CompanyResponse(String name) {

	public static CompanyResponse from(Company company) {
		return new CompanyResponse(company.getName());
	}
}
