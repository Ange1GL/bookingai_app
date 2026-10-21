package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.UpdateRoleCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.UpdateRoleUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicateRoleNameException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemRoleProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateRoleService implements UpdateRoleUseCase {

	private final RoleFinder roleFinder;
	private final RoleRepository roleRepository;

	@Override
	@Transactional
	public Role update(UpdateRoleCommand command) {
		Role current = roleFinder.getOrThrow(command.id());
		Role updated = current.withName(command.name()).withDescription(command.description());
		if (!updated.name().equals(current.name())) {
			ensureCanRename(current, updated);
		}
		return roleRepository.save(updated);
	}

	private void ensureCanRename(Role current, Role updated) {
		if (current.system()) {
			throw new SystemRoleProtectedException(
					"El rol '" + current.name() + "' es del sistema y no puede renombrarse");
		}
		if (roleRepository.existsByName(updated.name())) {
			throw new DuplicateRoleNameException(
					"Ya existe un rol con el nombre '" + updated.name() + "' (puede estar eliminado)");
		}
	}
}
