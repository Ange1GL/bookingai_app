package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.UpdatePermissionCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.UpdatePermissionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

import lombok.RequiredArgsConstructor;

/** Solo la descripción es editable: el nombre es el identificador que usa el código para autorizar. */
@Service
@RequiredArgsConstructor
public class UpdatePermissionService implements UpdatePermissionUseCase {

	private final PermissionFinder permissionFinder;
	private final PermissionRepositoryPort permissionRepository;

	@Override
	@Transactional
	public Permission update(UpdatePermissionCommand command) {
		Permission updated = permissionFinder.getOrThrow(command.id()).withDescription(command.description());
		return permissionRepository.save(updated);
	}
}
