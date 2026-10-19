package com.github.angellariosacosta.bookingapp.application.command;

import java.util.Set;

/** Conjunto completo de permisos que debe tener el rol: reemplaza al actual (lo que no esté se revoca). */
public record AssignRolePermissionsCommand(Long roleId, Set<Long> permissionIds) {
}
