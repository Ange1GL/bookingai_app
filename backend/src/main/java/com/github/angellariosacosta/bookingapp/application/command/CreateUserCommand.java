package com.github.angellariosacosta.bookingapp.application.command;

import java.util.Set;

public record CreateUserCommand(String username, String email, String name, String phone, Set<Long> roleIds) {
}
