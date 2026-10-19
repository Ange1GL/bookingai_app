package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

public record PermissionResponse(Long id, String name, String description, boolean active, boolean system) {
}
