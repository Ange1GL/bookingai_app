package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListPriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListPriceCatalogService implements ListPriceCatalogUseCase {

	private final PriceCatalogRepositoryPort priceCatalogRepository;

	@Override
	@Transactional(readOnly = true)
	public List<PriceCatalog> list(Long userId) {
		return priceCatalogRepository.findAllByUser(userId);
	}
}
