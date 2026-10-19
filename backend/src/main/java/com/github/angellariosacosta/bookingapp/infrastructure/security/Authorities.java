package com.github.angellariosacosta.bookingapp.infrastructure.security;

import com.github.angellariosacosta.bookingapp.domain.model.Permission;

/** Nombres de permisos (authorities) que exigen los endpoints administrativos; coinciden con la tabla permissions. */
public final class Authorities {

    public static final String ADMIN_ALL = Permission.ADMIN_ALL_NAME;
    public static final String USER_READ = "USER_READ";
    public static final String USER_WRITE = "USER_WRITE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String ROLE_READ = "ROLE_READ";
    public static final String ROLE_WRITE = "ROLE_WRITE";
    public static final String PERMISSION_READ = "PERMISSION_READ";
    public static final String PERMISSION_WRITE = "PERMISSION_WRITE";

    // Expresiones para @PreAuthorize (deben ser constantes de compilación): el permiso específico o ADMIN_ALL.
    public static final String CAN_READ_PERMISSIONS =
            "hasAnyAuthority('" + PERMISSION_READ + "', '" + ADMIN_ALL + "')";
    public static final String CAN_WRITE_PERMISSIONS =
            "hasAnyAuthority('" + PERMISSION_WRITE + "', '" + ADMIN_ALL + "')";

    public static final String CAN_READ_ROLES =
            "hasAnyAuthority('" + ROLE_READ + "', '" + ADMIN_ALL + "')";
    public static final String CAN_WRITE_ROLES =
            "hasAnyAuthority('" + ROLE_WRITE + "', '" + ADMIN_ALL + "')";

    private Authorities() {
    }
}
