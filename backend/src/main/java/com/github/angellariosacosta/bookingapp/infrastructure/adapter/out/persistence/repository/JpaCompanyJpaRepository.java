package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CompanyEntity;

public interface JpaCompanyJpaRepository extends JpaRepository<CompanyEntity, Long> {

	Optional<CompanyEntity> findByUserId(Long userId);

	boolean existsByUserId(Long userId);

	// Containing escapa los comodines (% y _) del texto que escribe el cliente.
	List<CompanyEntity> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
