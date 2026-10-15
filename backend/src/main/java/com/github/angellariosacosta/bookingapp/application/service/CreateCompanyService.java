package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreateCompanyCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateCompanyUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CompanyRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CompanyAlreadyExistsException;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateCompanyService implements CreateCompanyUseCase {

	private final CompanyRepositoryPort companyRepository;

	@Override
	@Transactional
	public Company create(CreateCompanyCommand command) {
		Company company = new Company(command.userId(), command.name());
		if (companyRepository.existsByUserId(command.userId())) {
			throw new CompanyAlreadyExistsException("User already has a company");
		}
		return companyRepository.save(company);
	}
}
