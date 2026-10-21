package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ChangeRoleStatusUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeRoleStatusService implements ChangeRoleStatusUseCase {

	private final RoleFinder roleFinder;
	private final RoleDeactivationGuard deactivationGuard;
	private final RoleRepository roleRepository;

	@Override
	@Transactional
	public Role enable(Long id) {
		Role role = roleFinder.getOrThrow(id);
		if (role.active()) {
			return role;
		}
		return roleRepository.save(role.withActive(true));
	}

	@Override
	@Transactional
	public Role disable(Long id) {
		Role role = roleFinder.getOrThrow(id);
		if (!role.active()) {
			return role;
		}
		deactivationGuard.ensureCanDeactivate(role);
		return roleRepository.save(role.withActive(false));
	}
}
