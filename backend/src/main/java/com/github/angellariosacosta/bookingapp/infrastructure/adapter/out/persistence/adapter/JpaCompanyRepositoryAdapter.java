package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CompanyRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CompanyAlreadyExistsException;
import com.github.angellariosacosta.bookingapp.domain.model.Company;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.CompanyMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaCompanyJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaCompanyRepositoryAdapter implements CompanyRepositoryPort {

	private static final String UNIQUE_USER_CONSTRAINT = "uk_company_user_id";

	private final JpaCompanyJpaRepository jpaRepository;
	private final CompanyMapper mapper;

	// saveAndFlush para traducir aquí la violación de la restricción única (dos requests simultáneos del mismo usuario).
	@Override
	public Company save(Company company) {
		try {
			return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(company)));
		} catch (DataIntegrityViolationException ex) {
			if (isDuplicateUserViolation(ex)) {
				throw new CompanyAlreadyExistsException("User already has a company");
			}
			throw ex;
		}
	}

	private boolean isDuplicateUserViolation(DataIntegrityViolationException ex) {
		String cause = ex.getMostSpecificCause().getMessage();
		return cause != null && cause.contains(UNIQUE_USER_CONSTRAINT);
	}

	@Override
	public Optional<Company> findByUserId(Long userId) {
		return jpaRepository.findByUserId(userId).map(mapper::toDomain);
	}

	@Override
	public boolean existsByUserId(Long userId) {
		return jpaRepository.existsByUserId(userId);
	}

	@Override
	public List<Company> searchByName(String name) {
		return jpaRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(name).stream()
				.map(mapper::toDomain)
				.toList();
	}
}
