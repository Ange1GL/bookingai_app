package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record RoleResponse(
		Long id,
		String name,
		String description,
		boolean active,
		boolean system,
		List<PermissionResponse> permissions
) {
}
