package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.AssignRolePermissionsCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.AssignRolePermissionsUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignRolePermissionsService implements AssignRolePermissionsUseCase {

	private final RoleFinder roleFinder;
	private final RolePermissionsAssigner permissionsAssigner;

	@Override
	@Transactional
	public Role assign(AssignRolePermissionsCommand command) {
		Role role = roleFinder.getOrThrow(command.roleId());
		permissionsAssigner.assign(role, command.permissionIds());
		return roleFinder.getOrThrow(role.id());
	}
}
