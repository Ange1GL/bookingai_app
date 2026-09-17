package com.github.angellariosacosta.bookingapp.domain.model;

import lombok.*;

import java.io.Serializable;
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

    private String password;

    private boolean active;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
