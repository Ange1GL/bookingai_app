package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.PermissionInUseException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemPermissionProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

import lombok.RequiredArgsConstructor;

/** Reglas comunes a deshabilitar y eliminar un permiso. */
@Component
@RequiredArgsConstructor
class PermissionDeactivationGuard {

	private final PermissionRepositoryPort permissionRepository;

	void ensureCanDeactivate(Permission permission) {
		if (permission.system()) {
			throw new SystemPermissionProtectedException(
					"El permiso '" + permission.name() + "' es del sistema y no puede deshabilitarse ni eliminarse");
		}
		if (permissionRepository.isAssignedToRole(permission.id())) {
			throw new PermissionInUseException("El permiso '" + permission.name()
					+ "' está asignado a uno o más roles y no puede deshabilitarse ni eliminarse");
		}
	}
}
