package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.config.CustomUserDetails;

import tools.jackson.databind.json.JsonMapper;

class PasswordChangeRequiredFilterTest {

	private final PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter(JsonMapper.builder().build());

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private void authenticateAs(boolean mustChangePassword) {
		User user = User.builder().id(1L).username("maria").active(true).mustChangePassword(mustChangePassword)
				.roles(Set.of()).build();
		CustomUserDetails details = new CustomUserDetails(user);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
	}

	private MockFilterChain run(String method, String path, MockHttpServletResponse response) throws Exception {
		MockFilterChain chain = new MockFilterChain();
		filter.doFilter(new MockHttpServletRequest(method, path), response, chain);
		return chain;
	}

	@Test
	void blocksEverythingElseWhileThePasswordIsTemporary() throws Exception {
		authenticateAs(true);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterChain chain = run("GET", "/api/v1/customers", response);

		assertEquals(403, response.getStatus());
		assertEquals(PasswordChangeRequiredFilter.ERROR_CODE,
				response.getHeader(PasswordChangeRequiredFilter.ERROR_CODE_HEADER));
		assertTrue(response.getContentAsString().contains("\"status\":403"));
		assertNull(chain.getRequest());
	}

	@Test
	void blocksAdminEndpointsToo() throws Exception {
		authenticateAs(true);
		MockHttpServletResponse response = new MockHttpServletResponse();

		run("POST", "/api/v1/admin/users", response);

		assertEquals(403, response.getStatus());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/auth/change-password", "/api/v1/auth/logout", "/api/v1/auth/refresh",
			"/api/v1/auth/csrf"})
	void letsThePasswordChangeAndSessionEndpointsThrough(String path) throws Exception {
		authenticateAs(true);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterChain chain = run("POST", path, response);

		assertEquals(200, response.getStatus());
		assertNotNull(chain.getRequest());
	}

	@Test
	void doesNotAffectUsersWithARegularPassword() throws Exception {
		authenticateAs(false);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterChain chain = run("GET", "/api/v1/customers", response);

		assertEquals(200, response.getStatus());
		assertNotNull(chain.getRequest());
	}

	@Test
	void doesNotAffectUnauthenticatedRequests() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterChain chain = run("POST", "/api/v1/auth/login", response);

		assertEquals(200, response.getStatus());
		assertNotNull(chain.getRequest());
	}

	@Test
	void pathMatchingIsExactSoLookalikePathsStayBlocked() throws Exception {
		authenticateAs(true);
		MockHttpServletResponse response = new MockHttpServletResponse();

		run("POST", "/api/v1/auth/change-password/../../customers", response);

		assertEquals(403, response.getStatus());
	}
}
