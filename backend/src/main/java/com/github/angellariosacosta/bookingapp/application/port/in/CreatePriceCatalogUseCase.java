package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreatePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

public interface CreatePriceCatalogUseCase {
	PriceCatalog create(CreatePriceCatalogCommand command);
}
