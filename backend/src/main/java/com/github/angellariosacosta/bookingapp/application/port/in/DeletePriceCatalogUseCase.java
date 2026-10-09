package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.DeletePriceCatalogCommand;

public interface DeletePriceCatalogUseCase {
	void delete(DeletePriceCatalogCommand command);
}
