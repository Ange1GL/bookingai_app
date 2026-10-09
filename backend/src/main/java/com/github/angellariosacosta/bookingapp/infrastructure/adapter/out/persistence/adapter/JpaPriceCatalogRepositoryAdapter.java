package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePriceCatalogLabelException;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.PriceCatalogMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaPriceCatalogJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaPriceCatalogRepositoryAdapter implements PriceCatalogRepositoryPort {

	private static final int NO_EXCLUDED_ID = 0;
	private static final String UNIQUE_LABEL_INDEX = "uk_price_catalog_user_label_active";

	private final JpaPriceCatalogJpaRepository jpaRepository;
	private final PriceCatalogMapper mapper;

	@Override
	public Optional<PriceCatalog> findById(Integer id, Long userId) {
		return jpaRepository.findOwnedById(id, userId).map(mapper::toDomain);
	}

	@Override
	public List<PriceCatalog> findAllByUser(Long userId) {
		return jpaRepository.findAllOwned(userId).stream().map(mapper::toDomain).toList();
	}

	// saveAndFlush fuerza el INSERT/UPDATE aqui para poder traducir la violacion del indice unico
	// (dos requests simultaneos que pasaron existsActiveByLabel); con save() el error saldria al commit.
	@Override
	public PriceCatalog save(PriceCatalog priceCatalog) {
		try {
			return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(priceCatalog)));
		} catch (DataIntegrityViolationException ex) {
			if (isDuplicateLabelViolation(ex)) {
				throw new DuplicatePriceCatalogLabelException(
						"Price catalog already exists with label: " + priceCatalog.getLabel());
			}
			throw ex;
		}
	}

	private boolean isDuplicateLabelViolation(DataIntegrityViolationException ex) {
		String cause = ex.getMostSpecificCause().getMessage();
		return cause != null && cause.contains(UNIQUE_LABEL_INDEX);
	}

	@Override
	public boolean existsActiveByLabel(String label, Long userId, Integer excludeId) {
		return jpaRepository.existsActiveByLabel(label, userId, excludeId == null ? NO_EXCLUDED_ID : excludeId);
	}
}
