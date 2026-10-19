package com.github.angellariosacosta.bookingapp.application.command;

/** {@code phone} null = no cambiar el teléfono actual. El username es inmutable. */
public record UpdateUserCommand(Long id, String name, String email, String phone) {
}
