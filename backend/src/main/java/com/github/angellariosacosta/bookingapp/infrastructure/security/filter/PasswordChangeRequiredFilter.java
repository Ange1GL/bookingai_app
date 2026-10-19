package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ErrorResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.config.CustomUserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mientras el usuario tenga una contraseña temporal ({@code must_change_password}), rechaza con 403 todo salvo cambiarla,
 * renovar/cerrar sesión y pedir el token CSRF. Lo hace cumplir el backend: no depende de que el frontend redirija.
 * El motivo viaja en la cabecera {@link #ERROR_CODE_HEADER} para que el cliente lo distinga de otros 403.
 *
 * <p>Se instancia en {@code SecurityConfig} (no es un bean) para que el contenedor no lo registre dos veces.
 */
@Slf4j
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

	public static final String ERROR_CODE_HEADER = "X-Error-Code";
	public static final String ERROR_CODE = "PASSWORD_CHANGE_REQUIRED";
	static final String MESSAGE = "Debes cambiar tu contraseña temporal antes de continuar";
	private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

	public static final String CHANGE_PASSWORD_PATH = "/api/v1/auth/change-password";
	private static final Set<String> ALLOWED_PATHS = Set.of(
			CHANGE_PASSWORD_PATH,
			"/api/v1/auth/logout",
			"/api/v1/auth/refresh",
			"/api/v1/auth/csrf");

	private final JsonMapper jsonMapper;

	public PasswordChangeRequiredFilter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (mustChangePassword() && !isAllowed(request)) {
			log.warn("Acceso bloqueado por contraseña temporal a {} {}", request.getMethod(), request.getRequestURI());
			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.setHeader(ERROR_CODE_HEADER, ERROR_CODE);
			response.setContentType(JSON_CONTENT_TYPE);
			jsonMapper.writeValue(response.getWriter(), ErrorResponse.of(HttpStatus.FORBIDDEN, MESSAGE));
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean mustChangePassword() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.getPrincipal() instanceof CustomUserDetails details
				&& details.user().isMustChangePassword();
	}

	private boolean isAllowed(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return ALLOWED_PATHS.contains(path);
	}
}
