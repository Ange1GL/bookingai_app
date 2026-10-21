package com.github.angellariosacosta.bookingapp.application.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.BootstrapAdminUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.result.BootstrapAdminResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BootstrapAdminService implements BootstrapAdminUseCase {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	@Override
	@Transactional
	public BootstrapAdminResult ensureAdmin(String username) {
		Optional<User> found = userRepository.findByUsername(username);
		if (found.isEmpty()) {
			return BootstrapAdminResult.USER_NOT_FOUND;
		}
		User user = found.get();
		boolean alreadyAdmin = user.getRoles().stream().anyMatch(role -> Role.ADMIN_NAME.equals(role.name()));
		if (alreadyAdmin) {
			return BootstrapAdminResult.ALREADY_ADMIN;
		}

		Role adminRole = roleRepository.findByName(Role.ADMIN_NAME)
				.orElseThrow(() -> new IllegalStateException("Role " + Role.ADMIN_NAME + " not found"));
		Set<Role> roles = new HashSet<>(user.getRoles());
		roles.add(adminRole);
		user.setRoles(roles);
		userRepository.save(user);
		return BootstrapAdminResult.GRANTED;
	}
}
