package com.github.angellariosacosta.bookingapp.domain.model;

import java.util.Set;

public record Role(Long id, String name, String description, boolean active, boolean system, Set<Permission> permissions) {

    public static final String ADMIN_NAME = "ADMIN";
    public static final String USER_NAME = "USER";
}
