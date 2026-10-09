package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.List;
import java.util.Optional;

import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

public interface PriceCatalogRepositoryPort {
	// Devuelven solo servicios activos del usuario mas los globales (user_id NULL).
	Optional<PriceCatalog> findById(Integer id, Long userId);
	List<PriceCatalog> findAllByUser(Long userId);
	PriceCatalog save(PriceCatalog priceCatalog);
	// excludeId puede ser null (alta); sirve para ignorar el propio servicio al editar.
	boolean existsActiveByLabel(String label, Long userId, Integer excludeId);
}
