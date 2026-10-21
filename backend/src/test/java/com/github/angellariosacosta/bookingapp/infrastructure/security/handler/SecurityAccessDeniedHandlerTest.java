package com.github.angellariosacosta.bookingapp.infrastructure.security.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import tools.jackson.databind.json.JsonMapper;

class SecurityAccessDeniedHandlerTest {

	@Test
	void writesForbiddenErrorResponse() throws Exception {
		SecurityAccessDeniedHandler handler = new SecurityAccessDeniedHandler(JsonMapper.builder().build());
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.handle(new MockHttpServletRequest("GET", "/api/v1/users"), response,
				new AccessDeniedException("denied"));

		assertEquals(403, response.getStatus());
		assertTrue(response.getContentType().startsWith("application/json"));
		String body = response.getContentAsString();
		assertTrue(body.contains("\"status\":403"));
		assertTrue(body.contains("Forbidden"));
	}
}
