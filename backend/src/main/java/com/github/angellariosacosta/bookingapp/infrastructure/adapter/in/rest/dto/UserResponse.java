package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record UserResponse(
		Long id,
		String username,
		String name,
		String lastName,
		String email,
		String phone,
		boolean active,
		boolean passwordSet,
		List<RoleSummaryResponse> roles
) {
}
