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

	// excludeId = 0 cuando no hay que excluir ninguno (los ids reales son > 0).
	@Query("""
			SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
			FROM PriceCatalogEntity p
			WHERE p.userId = :userId
			AND p.active = true
			AND LOWER(p.label) = LOWER(:label)
			AND p.id <> :excludeId
			""")
	boolean existsActiveByLabel(
			@Param("label") String label,
			@Param("userId") Long userId,
			@Param("excludeId") Integer excludeId);
}
