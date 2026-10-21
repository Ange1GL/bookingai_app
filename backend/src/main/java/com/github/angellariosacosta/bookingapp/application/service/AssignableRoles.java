package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

/** Resuelve y valida los roles que se pueden asignar a un usuario: al menos uno, todos existentes y activos. */
@Component
@RequiredArgsConstructor
class AssignableRoles {

	private static final String FIELD = "roleIds";

	private final RoleRepository roleRepository;

	List<Role> resolve(Set<Long> roleIds) {
		Set<Long> requested = roleIds == null ? Set.of() : Set.copyOf(roleIds);
		if (requested.isEmpty()) {
			throw new InvalidFieldException(FIELD, "at least one role is required");
		}
		List<Role> found = roleRepository.findAllByIds(requested);
		ensureAllExist(requested, found);
		ensureAllActive(found);
		return found;
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
}
