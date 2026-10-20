package com.github.angellariosacosta.bookingapp.domain.model;

import java.time.Instant;

/** Solicitud pendiente de un usuario que olvidó su contraseña: el administrador la atiende reiniciándola. */
public record PasswordRecoveryRequest(Long id, User user, Instant requestedAt) {
}
