package com.github.angellariosacosta.bookingapp.application.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.LastAdminProtectedException;
import com.github.angellariosacosta.bookingapp.domain.exception.SelfModificationException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

/** Protecciones que impiden que un administrador se quede (o deje al sistema) sin acceso administrativo. */
@Component
@RequiredArgsConstructor
class UserAdminGuard {

	private final UserRepository userRepository;

	void ensureNotSelf(Long actorId, Long targetId, String message) {
		if (Objects.equals(actorId, targetId)) {
			throw new SelfModificationException(message);
		}
	}

	boolean isAdmin(User user) {
		return user.getRoles().stream().anyMatch(Role::isAdmin);
	}

	/** Falla si {@code target} es un admin activo y no queda ningún otro admin activo. */
	void ensureAnotherActiveAdminExists(User target) {
		if (!target.isActive() || !isAdmin(target)) {
			return;
		}
		if (userRepository.countActiveUsersWithRole(Role.ADMIN_NAME, target.getId()) == 0) {
			throw new LastAdminProtectedException(
					"Debe existir al menos un administrador activo: este es el último");
		}
	}
}
