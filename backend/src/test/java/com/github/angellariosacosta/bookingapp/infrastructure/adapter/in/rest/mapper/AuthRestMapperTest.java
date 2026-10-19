package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AuthSuccessResponse;

class AuthRestMapperTest {

	private final AuthRestMapper mapper = new AuthRestMapper();

	@Test
	void exposesTheMustChangePasswordFlagButNeverTheTokens() {
		AuthTokenResult result = new AuthTokenResult("access", "refresh", 1L, "maria", "m@example.com", List.of("USER"), true);

		AuthSuccessResponse response = mapper.toResponse(result);

		assertTrue(response.mustChangePassword());
		assertFalse(response.toString().contains("access"));
		assertFalse(response.toString().contains("refresh"));
	}

	@Test
	void flagIsFalseForRegularSessions() {
		AuthTokenResult result = new AuthTokenResult("a", "r", 1L, "maria", "m@example.com", List.of("USER"), false);

		assertFalse(mapper.toResponse(result).mustChangePassword());
	}
}
