package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.DeletePermissionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeletePermissionService implements DeletePermissionUseCase {

	private final PermissionFinder permissionFinder;
	private final PermissionDeactivationGuard deactivationGuard;
	private final PermissionRepositoryPort permissionRepository;

	@Override
	@Transactional
	public void delete(Long id) {
		Permission permission = permissionFinder.getOrThrow(id);
		deactivationGuard.ensureCanDeactivate(permission);
		permissionRepository.markDeleted(permission.id());
	}
}
