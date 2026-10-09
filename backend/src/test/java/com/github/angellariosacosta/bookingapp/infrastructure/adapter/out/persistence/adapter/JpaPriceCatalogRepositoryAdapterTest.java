package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePriceCatalogLabelException;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PriceCatalogEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.PriceCatalogMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaPriceCatalogJpaRepository;

class JpaPriceCatalogRepositoryAdapterTest {

	private static final String UNIQUE_LABEL_INDEX = "uk_price_catalog_user_label_active";

	private JpaPriceCatalogJpaRepository jpaRepository;
	private PriceCatalogMapper mapper;
	private JpaPriceCatalogRepositoryAdapter adapter;
	private PriceCatalog priceCatalog;

	@BeforeEach
	void setUp() {
		jpaRepository = mock(JpaPriceCatalogJpaRepository.class);
		mapper = mock(PriceCatalogMapper.class);
		adapter = new JpaPriceCatalogRepositoryAdapter(jpaRepository, mapper);
		priceCatalog = PriceCatalog.createNew("Corte", 100, 1L);
		when(mapper.toEntity(priceCatalog)).thenReturn(new PriceCatalogEntity());
	}

	@Test
	void saveTranslatesUniqueIndexViolationToDuplicateLabel() {
		when(jpaRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
				"insert failed", new RuntimeException("duplicate key value violates unique constraint \"" + UNIQUE_LABEL_INDEX + "\"")));

		assertThrows(DuplicatePriceCatalogLabelException.class, () -> adapter.save(priceCatalog));
	}

	@Test
	void saveRethrowsOtherIntegrityViolations() {
		DataIntegrityViolationException other = new DataIntegrityViolationException(
				"insert failed", new RuntimeException("null value in column \"label\" violates not-null constraint"));
		when(jpaRepository.saveAndFlush(any())).thenThrow(other);

		assertSame(other, assertThrows(DataIntegrityViolationException.class, () -> adapter.save(priceCatalog)));
	}
}
