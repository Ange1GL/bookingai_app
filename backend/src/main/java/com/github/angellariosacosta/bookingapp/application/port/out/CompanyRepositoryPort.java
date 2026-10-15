package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.List;
import java.util.Optional;

import com.github.angellariosacosta.bookingapp.application.result.CompanySearchResult;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

public interface CompanyRepositoryPort {
	Company save(Company company);
	Optional<Company> findByUserId(Long userId);
	boolean existsByUserId(Long userId);
	// Coincidencia parcial sin distinguir mayúsculas; solo dueños activos.
	List<CompanySearchResult> searchByName(String name);
	List<CompanySearchResult> searchByOwnerName(String ownerName);
}
