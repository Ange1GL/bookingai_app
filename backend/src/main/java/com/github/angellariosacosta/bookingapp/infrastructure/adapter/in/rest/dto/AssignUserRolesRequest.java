package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AssignUserRolesRequest(
		@NotEmpty(message = "El usuario debe tener al menos un rol")
		Set<@NotNull(message = "Los ids de roles no pueden ser nulos") Long> roleIds
) {
}
