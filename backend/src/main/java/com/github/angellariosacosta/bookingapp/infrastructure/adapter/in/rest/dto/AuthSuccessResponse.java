package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.List;

/** {@code mustChangePassword}: la contraseña es temporal y el frontend debe llevar al usuario a cambiarla. */
public record AuthSuccessResponse(Long userId, String username, String email, List<String> roles, boolean mustChangePassword) {}
