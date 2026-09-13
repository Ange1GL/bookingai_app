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
import java.time.Instant;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;
    private static final String MESSAGE= "Autenticación requerida";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {


        log.warn("Intento de acceso no autenticado a {} {}: {}",
                request.getMethod(), request.getRequestURI(),
                Objects.requireNonNullElse(authException.getMessage(), "sin detalle"));

        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                MESSAGE
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(JSON_CONTENT_TYPE);
        jsonMapper.writeValue(response.getWriter(), body);
    }
}
