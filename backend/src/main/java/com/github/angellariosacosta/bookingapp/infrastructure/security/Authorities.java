package com.github.angellariosacosta.bookingapp.infrastructure.security;

/** Nombres de permisos (authorities) que exigen los endpoints administrativos; coinciden con la tabla permissions. */
public final class Authorities {

    public static final String ADMIN_ALL = "ADMIN_ALL";
    public static final String USER_READ = "USER_READ";
    public static final String USER_WRITE = "USER_WRITE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String ROLE_READ = "ROLE_READ";
    public static final String ROLE_WRITE = "ROLE_WRITE";
    public static final String PERMISSION_READ = "PERMISSION_READ";
    public static final String PERMISSION_WRITE = "PERMISSION_WRITE";

    private Authorities() {
    }
}
