package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.UpdatePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.UpdatePriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePriceCatalogLabelException;
import com.github.angellariosacosta.bookingapp.domain.exception.PriceCatalogNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdatePriceCatalogService implements UpdatePriceCatalogUseCase {

	private final PriceCatalogRepositoryPort priceCatalogRepository;

	@Override
	@Transactional
	public PriceCatalog update(UpdatePriceCatalogCommand command) {
		PriceCatalog current = priceCatalogRepository.findById(command.id(), command.userId())
				.orElseThrow(() -> new PriceCatalogNotFoundException("Price catalog not found with id: " + command.id()));
		current.ensureOwnedBy(command.userId());

		PriceCatalog updated = current.update(command.label(), command.price());
		if (priceCatalogRepository.existsActiveByLabel(command.label(), command.userId(), command.id())) {
			throw new DuplicatePriceCatalogLabelException("Price catalog already exists with label: " + command.label());
		}
		return priceCatalogRepository.save(updated);
	}
}
