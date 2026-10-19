package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreatePermissionCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreatePermissionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePermissionNameException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreatePermissionService implements CreatePermissionUseCase {

	private final PermissionRepositoryPort permissionRepository;

	@Override
	@Transactional
	public Permission create(CreatePermissionCommand command) {
		Permission permission = Permission.create(command.name(), command.description());
		if (permissionRepository.existsByName(permission.name())) {
			throw new DuplicatePermissionNameException(
					"Ya existe un permiso con el nombre '" + permission.name() + "' (puede estar eliminado)");
		}
		return permissionRepository.save(permission);
	}
}
