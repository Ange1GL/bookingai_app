package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.RoleNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RoleFinder {

	private final RoleRepository roleRepository;

	Role getOrThrow(Long id) {
		return roleRepository.findById(id)
				.orElseThrow(() -> new RoleNotFoundException("Rol no encontrado: " + id));
	}
}
