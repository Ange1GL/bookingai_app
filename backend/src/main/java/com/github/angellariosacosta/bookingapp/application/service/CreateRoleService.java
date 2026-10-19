package com.github.angellariosacosta.bookingapp.application.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreateRoleCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateRoleUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicateRoleNameException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateRoleService implements CreateRoleUseCase {

	private final RoleRepository roleRepository;
	private final RolePermissionsAssigner permissionsAssigner;
	private final RoleFinder roleFinder;

	@Override
	@Transactional
	public Role create(CreateRoleCommand command) {
		Role role = Role.create(command.name(), command.description());
		if (roleRepository.existsByName(role.name())) {
			throw new DuplicateRoleNameException(
					"Ya existe un rol con el nombre '" + role.name() + "' (puede estar eliminado)");
		}
		Role saved = roleRepository.save(role);
		Set<Long> permissionIds = command.permissionIds();
		if (permissionIds == null || permissionIds.isEmpty()) {
			return saved;
		}
		permissionsAssigner.assign(saved, permissionIds);
		return roleFinder.getOrThrow(saved.id());
	}
}
