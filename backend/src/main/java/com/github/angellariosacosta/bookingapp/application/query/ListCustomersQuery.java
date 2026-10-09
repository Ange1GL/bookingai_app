package com.github.angellariosacosta.bookingapp.application.query;

/**
 * Criterios de listado de clientes de un tenant. {@code name} y {@code phone} son filtros
 * opcionales de coincidencia parcial; {@code blacklisted} filtra por estado de lista negra
 * (true = solo bloqueados, false = solo no bloqueados). {@code null} significa "sin filtro".
 */
public record ListCustomersQuery(
		Long userId,
		String name,
		String phone,
		Boolean blacklisted,
		int page,
		int size,
		CustomerSortField sortBy,
		SortDirection direction
) {
}
