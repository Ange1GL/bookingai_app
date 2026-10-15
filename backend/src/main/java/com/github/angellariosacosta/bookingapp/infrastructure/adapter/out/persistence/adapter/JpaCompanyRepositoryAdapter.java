package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CompanyRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.CompanySearchResult;
import com.github.angellariosacosta.bookingapp.domain.model.Company;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.CompanyMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaCompanyJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaCompanyRepositoryAdapter implements CompanyRepositoryPort {

	private final JpaCompanyJpaRepository jpaRepository;
	private final CompanyMapper mapper;

	@Override
	public Company save(Company company) {
		return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(company)));
	}

	@Override
	public Optional<Company> findByUserId(Long userId) {
		return jpaRepository.findById(userId).map(mapper::toDomain);
	}

	@Override
	public boolean existsByUserId(Long userId) {
		return jpaRepository.existsById(userId);
	}

	@Override
	public List<CompanySearchResult> searchByName(String name) {
		return toResults(jpaRepository.findTop10ByUser_ActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(name));
	}

	@Override
	public List<CompanySearchResult> searchByOwnerName(String ownerName) {
		return toResults(jpaRepository
				.findTop10ByUser_ActiveTrueAndUser_NameContainingIgnoreCaseOrderByUser_NameAscNameAsc(ownerName));
	}

	private List<CompanySearchResult> toResults(List<CompanyEntity> entities) {
		return entities.stream().map(mapper::toSearchResult).toList();
	}
}
