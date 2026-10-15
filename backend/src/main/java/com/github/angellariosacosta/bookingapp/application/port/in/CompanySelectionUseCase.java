package com.github.angellariosacosta.bookingapp.application.port.in;

import java.util.Optional;

import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;

public interface CompanySelectionUseCase {
	// El negocio se identifica por el id de su dueño. Lanza CompanyNotFoundException si ese usuario no tiene negocio.
	CompanySelection select(String conversationKey, Long ownerUserId);

	Optional<CompanySelection> current(String conversationKey);
}
