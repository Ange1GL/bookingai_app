package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.RoleInUseException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemRoleProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

/** Reglas comunes a deshabilitar y eliminar un rol. */
@Component
@RequiredArgsConstructor
class RoleDeactivationGuard {

	private final RoleRepository roleRepository;

	void ensureCanDeactivate(Role role) {
		if (role.system()) {
			throw new SystemRoleProtectedException(
					"El rol '" + role.name() + "' es del sistema y no puede deshabilitarse ni eliminarse");
		}
		if (roleRepository.isAssignedToUser(role.id())) {
			throw new RoleInUseException("El rol '" + role.name()
					+ "' está asignado a uno o más usuarios y no puede deshabilitarse ni eliminarse");
		}
	}
}
