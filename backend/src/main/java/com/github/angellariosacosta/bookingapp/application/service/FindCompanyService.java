package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.FindCompanyUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CompanyRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CompanyNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindCompanyService implements FindCompanyUseCase {

	private final CompanyRepositoryPort companyRepository;

	@Override
	@Transactional(readOnly = true)
	public Company findByOwner(Long userId) {
		return companyRepository.findByUserId(userId)
				.orElseThrow(() -> new CompanyNotFoundException("Company not found for user: " + userId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Company> searchByName(String name) {
		if (name == null || name.isBlank()) {
			return List.of();
		}
		return companyRepository.searchByName(name.strip());
	}
}
