package com.github.angellariosacosta.bookingapp.infrastructure.security.entrypoint;


import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * Responds 401 with the standard {@link ErrorResponse} body when a request reaches a protected resource
 * without valid authentication. Spring Security's default would redirect to an HTML login page, which makes
 * no sense for a REST API.
 * <p>
 * Invoked from two places:
 * <ul>
 *   <li>Spring Security's {@code ExceptionTranslationFilter}, registered in {@code SecurityConfig} via
 *       {@code exceptionHandling(...).authenticationEntryPoint(...)}, when an anonymous request hits a
 *       protected route.</li>
 *   <li>{@code JwtAuthenticationFilter}, directly, when the token is invalid or expired, so both cases
 *       return the same 401.</li>
 * </ul>
 * It runs in the filter chain, before any controller, so it writes to the response itself instead of going
 * through {@code GlobalExceptionHandler}. The authentication failure detail is only logged, never sent to the
 * client. A 403 (authenticated but without permission) is handled by {@code SecurityAccessDeniedHandler}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;
    private static final String MESSAGE = "Autenticación requerida";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {


        log.warn("Intento de acceso no autenticado a {} {}: {}",
                request.getMethod(), request.getRequestURI(),
                Objects.requireNonNullElse(authException.getMessage(), "sin detalle"));

        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED, MESSAGE);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(JSON_CONTENT_TYPE);
        jsonMapper.writeValue(response.getWriter(), body);
    }
}
