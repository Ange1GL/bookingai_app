package com.github.angellariosacosta.bookingapp.domain.model;

import java.util.Set;

public record Role(Long id, String name, String description, boolean active, boolean system, Set<Permission> permissions) {

	public static final String ADMIN_NAME = "ADMIN";
	public static final String USER_NAME = "USER";
	public static final int MAX_NAME_LENGTH = 50;
	public static final int MAX_DESCRIPTION_LENGTH = CatalogFields.MAX_DESCRIPTION_LENGTH;

	/** Rol nuevo: activo, sin permisos y nunca de sistema (los roles base se siembran por migración). */
	public static Role create(String name, String description) {
		return new Role(null, CatalogFields.normalizeName(name, MAX_NAME_LENGTH),
				CatalogFields.normalizeDescription(description), true, false, Set.of());
	}

	public Role withName(String newName) {
		return new Role(id, CatalogFields.normalizeName(newName, MAX_NAME_LENGTH), description, active, system, permissions);
	}

	public Role withDescription(String newDescription) {
		return new Role(id, name, CatalogFields.normalizeDescription(newDescription), active, system, permissions);
	}

	public Role withActive(boolean newActive) {
		return new Role(id, name, description, newActive, system, permissions);
	}

	public boolean isAdmin() {
		return ADMIN_NAME.equals(name);
	}
}
