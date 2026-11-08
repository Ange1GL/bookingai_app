package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.github.angellariosacosta.bookingapp.application.port.out.AccessTokenRevocationRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.application.result.DecodedToken;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.config.CookieProperties;
import com.github.angellariosacosta.bookingapp.infrastructure.config.CustomUserDetails;
import com.github.angellariosacosta.bookingapp.infrastructure.security.entrypoint.SecurityEntryPoint;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtAuthenticationException;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtErrorCode;

class JwtAuthenticationFilterTest {

	private static final String USERNAME = "owner";
	private static final String JTI = "jti-1";

	private TokenService tokenService;
	private UserDetailsService userDetailsService;
	private SecurityEntryPoint entryPoint;
	private JwtAuthenticationFilter filter;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private MockFilterChain chain;

	@BeforeEach
	void setUp() {
		tokenService = mock(TokenService.class);
		userDetailsService = mock(UserDetailsService.class);
		entryPoint = mock(SecurityEntryPoint.class);
		AccessTokenRevocationRepositoryPort revocations = mock(AccessTokenRevocationRepositoryPort.class);
		filter = new JwtAuthenticationFilter(tokenService, userDetailsService, new CookieProperties(), entryPoint,
				revocations);

		request = new MockHttpServletRequest("GET", "/api/v1/customers");
		request.addHeader("Authorization", "Bearer token");
		response = new MockHttpServletResponse();
		chain = new MockFilterChain();

		stubDecodedToken(0);
		when(revocations.existsByJti(JTI)).thenReturn(false);
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void rejectsDisabledUserWithUserDisabledError() throws Exception {
		stubUser(false);

		filter.doFilter(request, response, chain);

		ArgumentCaptor<AuthenticationException> error = ArgumentCaptor.forClass(AuthenticationException.class);
		verify(entryPoint).commence(any(), any(), error.capture());
		assertEquals(JwtErrorCode.USER_DISABLED, ((JwtAuthenticationException) error.getValue()).getErrorCode());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNull(chain.getRequest());
	}

	@Test
	void authenticatesEnabledUser() throws Exception {
		stubUser(true);

		filter.doFilter(request, response, chain);

		verify(entryPoint, never()).commence(any(), any(), any());
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		assertNotNull(chain.getRequest());
	}

	@Test
	void rejectsTokenFromAnOlderSessionVersion() throws Exception {
		stubUser(true, 1);
		stubDecodedToken(0);

		filter.doFilter(request, response, chain);

		ArgumentCaptor<AuthenticationException> error = ArgumentCaptor.forClass(AuthenticationException.class);
		verify(entryPoint).commence(any(), any(), error.capture());
		assertEquals(JwtErrorCode.SESSION_REVOKED, ((JwtAuthenticationException) error.getValue()).getErrorCode());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNull(chain.getRequest());
	}

	@Test
	void authenticatesTokenWithCurrentSessionVersion() throws Exception {
		stubUser(true, 3);
		stubDecodedToken(3);

		filter.doFilter(request, response, chain);

		verify(entryPoint, never()).commence(any(), any(), any());
		assertNotNull(chain.getRequest());
	}

	private void stubDecodedToken(int tokenVersion) {
		when(tokenService.decode("token"))
				.thenReturn(new DecodedToken(USERNAME, JTI, Instant.now().plusSeconds(60), tokenVersion));
	}

	private void stubUser(boolean active) {
		stubUser(active, 0);
	}

	private void stubUser(boolean active, int tokenVersion) {
		User user = User.builder().id(1L).username(USERNAME).active(active).tokenVersion(tokenVersion)
				.roles(Set.of()).build();
		when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(new CustomUserDetails(user));
	}
}
