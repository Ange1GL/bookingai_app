package com.github.angellariosacosta.bookingapp.domain.model;

import java.util.Set;

public record Role(Long id, String name, String description, Set<Permission> permissions) {}
