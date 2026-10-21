package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.DeleteRoleUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteRoleService implements DeleteRoleUseCase {

	private final RoleFinder roleFinder;
	private final RoleDeactivationGuard deactivationGuard;
	private final RoleRepository roleRepository;

	@Override
	@Transactional
	public void delete(Long id) {
		Role role = roleFinder.getOrThrow(id);
		deactivationGuard.ensureCanDeactivate(role);
		roleRepository.markDeleted(role.id());
	}
}
