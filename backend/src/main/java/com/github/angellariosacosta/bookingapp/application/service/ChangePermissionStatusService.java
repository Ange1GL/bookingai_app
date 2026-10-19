package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ChangePermissionStatusUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangePermissionStatusService implements ChangePermissionStatusUseCase {

	private final PermissionFinder permissionFinder;
	private final PermissionDeactivationGuard deactivationGuard;
	private final PermissionRepositoryPort permissionRepository;

	@Override
	@Transactional
	public Permission enable(Long id) {
		Permission permission = permissionFinder.getOrThrow(id);
		if (permission.active()) {
			return permission;
		}
		return permissionRepository.save(permission.withActive(true));
	}

	@Override
	@Transactional
	public Permission disable(Long id) {
		Permission permission = permissionFinder.getOrThrow(id);
		if (!permission.active()) {
			return permission;
		}
		deactivationGuard.ensureCanDeactivate(permission);
		return permissionRepository.save(permission.withActive(false));
	}
}
