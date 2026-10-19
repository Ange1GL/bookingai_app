package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.SystemRoleProtectedException;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

/** Valida y aplica el conjunto de permisos de un rol; lo comparten el alta y la reasignación. */
@Component
@RequiredArgsConstructor
class RolePermissionsAssigner {

	private static final String FIELD = "permissionIds";

	private final PermissionRepositoryPort permissionRepository;
	private final RoleRepository roleRepository;

	void assign(Role role, Set<Long> permissionIds) {
		Set<Long> requested = permissionIds == null ? Set.of() : Set.copyOf(permissionIds);
		List<Permission> found = permissionRepository.findAllByIds(requested);

		ensureAllExist(requested, found);
		ensureAllActive(found);
		ensureAdminKeepsFullAccess(role, found);

		roleRepository.replacePermissions(role.id(), requested);
	}

	private void ensureAllExist(Set<Long> requested, List<Permission> found) {
		Set<Long> foundIds = found.stream().map(Permission::id).collect(Collectors.toSet());
		TreeSet<Long> missing = new TreeSet<>(requested);
		missing.removeAll(foundIds);
		if (!missing.isEmpty()) {
			throw new InvalidFieldException(FIELD, "unknown permission ids " + missing);
		}
	}

	private void ensureAllActive(List<Permission> found) {
		List<String> inactive = found.stream().filter(permission -> !permission.active()).map(Permission::name).sorted().toList();
		if (!inactive.isEmpty()) {
			throw new InvalidFieldException(FIELD, "disabled permissions cannot be assigned " + inactive);
		}
	}

	private void ensureAdminKeepsFullAccess(Role role, List<Permission> found) {
		boolean keepsAdminAll = found.stream().anyMatch(permission -> Permission.ADMIN_ALL_NAME.equals(permission.name()));
		if (role.isAdmin() && !keepsAdminAll) {
			throw new SystemRoleProtectedException(
					"El rol '" + role.name() + "' no puede perder el permiso " + Permission.ADMIN_ALL_NAME);
		}
	}
}
