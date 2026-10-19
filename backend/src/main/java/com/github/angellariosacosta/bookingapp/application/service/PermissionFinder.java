package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.PermissionNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class PermissionFinder {

	private final PermissionRepositoryPort permissionRepository;

	Permission getOrThrow(Long id) {
		return permissionRepository.findById(id)
				.orElseThrow(() -> new PermissionNotFoundException("Permiso no encontrado: " + id));
	}
}
