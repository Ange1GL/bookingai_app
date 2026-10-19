package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.AssignUserRolesCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.AssignUserRolesUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignUserRolesService implements AssignUserRolesUseCase {

	private final UserFinder userFinder;
	private final UserAdminGuard adminGuard;
	private final AssignableRoles assignableRoles;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public User assign(AssignUserRolesCommand command) {
		User user = userFinder.getOrThrow(command.userId());
		List<Role> roles = assignableRoles.resolve(command.roleIds());
		ensureAdminAccessIsNotLost(command.actorId(), user, roles);

		userRepository.replaceRoles(user.getId(), Set.copyOf(command.roleIds()));
		return userFinder.getOrThrow(user.getId());
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
