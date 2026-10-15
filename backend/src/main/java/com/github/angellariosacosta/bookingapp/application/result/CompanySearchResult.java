package com.github.angellariosacosta.bookingapp.application.result;

/** Coincidencia de búsqueda: el negocio se identifica por el id de su dueño; el nombre del dueño ayuda a desambiguar. */
public record CompanySearchResult(Long ownerUserId, String companyName, String ownerName) {
}
