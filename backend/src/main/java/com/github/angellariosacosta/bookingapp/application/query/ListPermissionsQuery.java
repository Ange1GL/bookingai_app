package com.github.angellariosacosta.bookingapp.application.query;

/**
 * Criterios de listado de permisos. {@code search} coincide parcialmente con nombre o descripción;
 * {@code active} filtra por estado (null = todos). Los eliminados lógicamente nunca se listan.
 */
public record ListPermissionsQuery(
		String search,
		Boolean active,
		int page,
		int size,
		PermissionSortField sortBy,
		SortDirection direction
) {
}
