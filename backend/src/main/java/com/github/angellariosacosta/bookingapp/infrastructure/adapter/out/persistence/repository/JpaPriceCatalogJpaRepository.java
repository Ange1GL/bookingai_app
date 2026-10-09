package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PriceCatalogEntity;

public interface JpaPriceCatalogJpaRepository extends JpaRepository<PriceCatalogEntity, Integer> {

	// Cada usuario tiene su propio catalogo: solo servicios activos del usuario.
	@Query("SELECT p FROM PriceCatalogEntity p WHERE p.id = :id AND p.active = true AND p.userId = :userId")
	Optional<PriceCatalogEntity> findOwnedById(@Param("id") Integer id, @Param("userId") Long userId);

	@Query("SELECT p FROM PriceCatalogEntity p WHERE p.active = true AND p.userId = :userId ORDER BY p.id ASC")
	List<PriceCatalogEntity> findAllOwned(@Param("userId") Long userId);

	// Al crear: no hay registro propio que ignorar.
	boolean existsByUserIdAndActiveTrueAndLabelIgnoreCase(Long userId, String label);

	// Al editar: se ignora el propio registro para que no choque consigo mismo.
	boolean existsByUserIdAndActiveTrueAndLabelIgnoreCaseAndIdNot(Long userId, String label, Integer id);
}
