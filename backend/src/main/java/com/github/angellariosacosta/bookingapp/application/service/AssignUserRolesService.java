package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.AssignUserRolesCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.AssignUserRolesUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignUserRolesService implements AssignUserRolesUseCase {

	private static final String FIELD = "roleIds";

	private final UserFinder userFinder;
	private final UserAdminGuard adminGuard;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	@Override
	@Transactional
	public User assign(AssignUserRolesCommand command) {
		User user = userFinder.getOrThrow(command.userId());
		Set<Long> requested = command.roleIds() == null ? Set.of() : Set.copyOf(command.roleIds());
		if (requested.isEmpty()) {
			throw new InvalidFieldException(FIELD, "at least one role is required");
		}

		List<Role> roles = roleRepository.findAllByIds(requested);
		ensureAllExist(requested, roles);
		ensureAllActive(roles);
		ensureAdminAccessIsNotLost(command.actorId(), user, roles);

		userRepository.replaceRoles(user.getId(), requested);
		return userFinder.getOrThrow(user.getId());
	}

	private void ensureAllExist(Set<Long> requested, List<Role> found) {
		Set<Long> foundIds = found.stream().map(Role::id).collect(Collectors.toSet());
		TreeSet<Long> missing = new TreeSet<>(requested);
		missing.removeAll(foundIds);
		if (!missing.isEmpty()) {
			throw new InvalidFieldException(FIELD, "unknown role ids " + missing);
		}
	}

	private void ensureAllActive(List<Role> found) {
		List<String> inactive = found.stream().filter(role -> !role.active()).map(Role::name).sorted().toList();
		if (!inactive.isEmpty()) {
			throw new InvalidFieldException(FIELD, "disabled roles cannot be assigned " + inactive);
		}
	}

	private void ensureAdminAccessIsNotLost(Long actorId, User user, List<Role> newRoles) {
		boolean removesAdmin = adminGuard.isAdmin(user) && newRoles.stream().noneMatch(Role::isAdmin);
		if (!removesAdmin) {
			return;
		}
		adminGuard.ensureNotSelf(actorId, user.getId(), "No puedes quitarte tu propio rol " + Role.ADMIN_NAME);
		adminGuard.ensureAnotherActiveAdminExists(user);
	}
}
