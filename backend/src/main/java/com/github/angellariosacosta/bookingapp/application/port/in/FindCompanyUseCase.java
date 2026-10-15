package com.github.angellariosacosta.bookingapp.application.port.in;

import java.util.List;

import com.github.angellariosacosta.bookingapp.application.result.CompanySearchResult;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

public interface FindCompanyUseCase {
	// Lanza CompanyNotFoundException si el usuario aún no registró su compañía.
	Company findByOwner(Long userId);

	// Coincidencia parcial sin distinguir mayúsculas (máx. 10); lista vacía si el texto está en blanco.
	List<CompanySearchResult> searchByName(String name);

	// Igual, pero por el nombre del dueño; varios dueños con el mismo nombre devuelven varias coincidencias.
	List<CompanySearchResult> searchByOwnerName(String ownerName);
}
