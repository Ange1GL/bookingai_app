package com.github.angellariosacosta.bookingapp.application.command;

/** {@code email} es opcional (null o vacío lo borra). {@code phone} null = no cambiar el teléfono actual. El username es inmutable. */
public record UpdateUserCommand(Long id, String name, String lastName, String email, String phone) {
}
