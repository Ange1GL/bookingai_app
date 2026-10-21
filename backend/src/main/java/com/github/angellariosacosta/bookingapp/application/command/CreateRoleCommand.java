package com.github.angellariosacosta.bookingapp.application.command;

import java.util.Set;

public record CreateRoleCommand(String name, String description, Set<Long> permissionIds) {
}
