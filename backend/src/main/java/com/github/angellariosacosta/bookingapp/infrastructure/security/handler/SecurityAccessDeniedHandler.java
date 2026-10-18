package com.github.angellariosacosta.bookingapp.infrastructure.security.handler;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/** Responde 403 con el cuerpo {@link ErrorResponse} cuando un usuario autenticado carece del permiso exigido. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    private static final String MESSAGE = "No tienes permiso para realizar esta acción";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private final JsonMapper jsonMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Acceso denegado a {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(JSON_CONTENT_TYPE);
        jsonMapper.writeValue(response.getWriter(), ErrorResponse.of(HttpStatus.FORBIDDEN, MESSAGE));
    }
}
