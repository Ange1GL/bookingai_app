package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreatePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreatePriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePriceCatalogLabelException;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreatePriceCatalogService implements CreatePriceCatalogUseCase {

	private final PriceCatalogRepositoryPort priceCatalogRepository;

	@Override
	@Transactional
	public PriceCatalog create(CreatePriceCatalogCommand command) {
		PriceCatalog priceCatalog = PriceCatalog.createNew(command.label(), command.price(), command.userId());
		if (priceCatalogRepository.existsActiveByLabel(command.label(), command.userId(), null)) {
			throw new DuplicatePriceCatalogLabelException("Price catalog already exists with label: " + command.label());
		}
		return priceCatalogRepository.save(priceCatalog);
	}
}
