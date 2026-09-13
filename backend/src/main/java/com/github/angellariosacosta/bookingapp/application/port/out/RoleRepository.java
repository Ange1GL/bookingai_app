package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.domain.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByName(String name);
    Role save(Role role);
    List<Role> findAll();
}
