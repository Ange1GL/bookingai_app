package com.github.angellariosacosta.bookingapp.application.command;

import java.util.Set;

/** Conjunto completo de roles del usuario: reemplaza al actual. {@code actorId} es el admin que ejecuta la acción. */
public record AssignUserRolesCommand(Long actorId, Long userId, Set<Long> roleIds) {
}
