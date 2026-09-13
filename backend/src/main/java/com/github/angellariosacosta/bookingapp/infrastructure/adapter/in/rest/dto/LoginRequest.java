package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email no puede estar vacío")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía")
        String password
) {}
