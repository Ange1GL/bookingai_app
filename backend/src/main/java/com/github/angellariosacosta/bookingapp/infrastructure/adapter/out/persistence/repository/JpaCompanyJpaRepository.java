package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CompanyEntity;

public interface JpaCompanyJpaRepository extends JpaRepository<CompanyEntity, Long> {

	// Containing escapa los comodines (% y _) del texto que escribe el cliente; IgnoreCase no distingue mayúsculas.
	// EntityGraph trae al dueño en la misma consulta (el nombre se muestra para desambiguar).
	@EntityGraph(attributePaths = "user")
	List<CompanyEntity> findTop10ByUser_ActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);

	@EntityGraph(attributePaths = "user")
	List<CompanyEntity> findTop10ByUser_ActiveTrueAndUser_NameContainingIgnoreCaseOrderByUser_NameAscNameAsc(String ownerName);
}
