package com.github.angellariosacosta.bookingapp.application.query;

/**
 * Criterios de listado de usuarios. {@code search} coincide parcialmente con username, nombre, email o teléfono;
 * {@code active} filtra por estado y {@code roleId} por rol asignado (null = sin filtro). Los eliminados
 * lógicamente nunca se listan.
 */
public record ListUsersQuery(
		String search,
		Boolean active,
		Long roleId,
		int page,
		int size,
		UserSortField sortBy,
		SortDirection direction
) {
}
