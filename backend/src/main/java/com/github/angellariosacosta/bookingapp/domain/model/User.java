package com.github.angellariosacosta.bookingapp.domain.model;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;

    private String email;

    private String name;

    private String password;

    private boolean active;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
