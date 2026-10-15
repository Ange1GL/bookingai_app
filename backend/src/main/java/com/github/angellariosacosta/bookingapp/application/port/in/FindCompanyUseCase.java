package com.github.angellariosacosta.bookingapp.application.port.in;

import java.util.List;

import com.github.angellariosacosta.bookingapp.domain.model.Company;

public interface FindCompanyUseCase {
	// Lanza CompanyNotFoundException si el usuario aún no registró su compañía.
	Company findByOwner(Long userId);

	// Coincidencia parcial sin distinguir mayúsculas; lista vacía si el texto está en blanco.
	List<Company> searchByName(String name);
}
