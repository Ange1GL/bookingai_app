package com.github.angellariosacosta.bookingapp.application.port.in;

import java.util.Optional;

import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;

public interface CompanySelectionUseCase {
	// Lanza CompanyNotFoundException si el negocio no existe.
	CompanySelection select(String conversationKey, Long companyId);

	Optional<CompanySelection> current(String conversationKey);
}
