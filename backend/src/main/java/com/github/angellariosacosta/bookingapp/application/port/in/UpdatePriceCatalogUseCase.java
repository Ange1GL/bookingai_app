package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.UpdatePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

public interface UpdatePriceCatalogUseCase {
	PriceCatalog update(UpdatePriceCatalogCommand command);
}
