package com.github.angellariosacosta.bookingapp.domain.model;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    private Long id;

    // Identificador estable para login y para el JWT (claim "username"). A diferencia de email,
    // no cambia — por eso el email puede editarse libremente sin romper tokens ya emitidos.
    private String username;

    private String email;

    private String name;

    // null = cuenta sin contraseña: pendiente de crearla con un código de un solo uso (OTP) enviado por WhatsApp.
    private String password;

    private boolean active;

    // Teléfono en formato internacional (solo dígitos); destino del código de un solo uso por WhatsApp.
    private String phone;

    private Instant passwordChangedAt;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
