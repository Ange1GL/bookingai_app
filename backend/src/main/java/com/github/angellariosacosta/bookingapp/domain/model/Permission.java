package com.github.angellariosacosta.bookingapp.domain.model;

public record Permission(Long id, String name, String description, boolean active, boolean system) {

	public static final String ADMIN_ALL_NAME = "ADMIN_ALL";
	public static final int MAX_NAME_LENGTH = 100;
	public static final int MAX_DESCRIPTION_LENGTH = CatalogFields.MAX_DESCRIPTION_LENGTH;

	/** Permiso nuevo: activo y nunca de sistema (el catálogo base solo se siembra por migración). */
	public static Permission create(String name, String description) {
		return new Permission(null, CatalogFields.normalizeName(name, MAX_NAME_LENGTH),
				CatalogFields.normalizeDescription(description), true, false);
	}

	public Permission withDescription(String newDescription) {
		return new Permission(id, name, CatalogFields.normalizeDescription(newDescription), active, system);
	}

	public Permission withActive(boolean newActive) {
		return new Permission(id, name, description, newActive, system);
	}
}
