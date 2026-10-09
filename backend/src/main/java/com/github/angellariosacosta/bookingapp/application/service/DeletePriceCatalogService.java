package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.DeletePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.DeletePriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.PriceCatalogNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeletePriceCatalogService implements DeletePriceCatalogUseCase {

	private final PriceCatalogRepositoryPort priceCatalogRepository;

	// Baja logica: las citas existentes siguen referenciando el servicio.
	@Override
	@Transactional
	public void delete(DeletePriceCatalogCommand command) {
		PriceCatalog current = priceCatalogRepository.findById(command.id(), command.userId())
				.orElseThrow(() -> new PriceCatalogNotFoundException("Price catalog not found with id: " + command.id()));
		current.ensureOwnedBy(command.userId());
		priceCatalogRepository.save(current.deactivate());
	}
}
