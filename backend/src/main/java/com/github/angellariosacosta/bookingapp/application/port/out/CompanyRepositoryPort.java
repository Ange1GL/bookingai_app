package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.List;
import java.util.Optional;

import com.github.angellariosacosta.bookingapp.domain.model.Company;

public interface CompanyRepositoryPort {
	Company save(Company company);
	Optional<Company> findById(Long id);
	Optional<Company> findByUserId(Long userId);
	boolean existsByUserId(Long userId);
	List<Company> searchByName(String name);
}
