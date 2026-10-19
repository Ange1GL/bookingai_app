package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

public record AssignRolePermissionsRequest(
		@NotNull(message = "permissionIds es obligatorio (puede ser una lista vacía)")
		Set<@NotNull(message = "Los ids de permisos no pueden ser nulos") Long> permissionIds
) {
}
