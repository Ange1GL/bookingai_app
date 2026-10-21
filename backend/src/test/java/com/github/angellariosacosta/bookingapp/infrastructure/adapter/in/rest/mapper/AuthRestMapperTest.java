package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AuthSuccessResponse;

class AuthRestMapperTest {

	private final AuthRestMapper mapper = new AuthRestMapper();

	@Test
	void exposesTheProfileButNeverTheTokens() {
		AuthTokenResult result = new AuthTokenResult("access", "refresh", 1L, "maria", "m@example.com", List.of("USER"), List.of("USER_READ"));

		AuthSuccessResponse response = mapper.toResponse(result);

		assertEquals("maria", response.username());
		assertEquals(List.of("USER"), response.roles());
		assertEquals(List.of("USER_READ"), response.permissions());
		assertFalse(response.toString().contains("access"));
		assertFalse(response.toString().contains("refresh"));
	}
}
