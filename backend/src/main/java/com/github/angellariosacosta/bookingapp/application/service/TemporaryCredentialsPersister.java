package com.github.angellariosacosta.bookingapp.application.service;

import java.util.HashSet;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.TemporaryPasswordGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePhoneException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneNumbers;
import com.github.angellariosacosta.bookingapp.domain.shared.Usernames;

import lombok.RequiredArgsConstructor;

/**
 * Parte transaccional de emitir una contraseña temporal. Se separa de la entrega por WhatsApp para que el mensaje
 * solo salga cuando la transacción ya hizo commit (no se envía una clave de un usuario que no llegó a guardarse).
 */
@Component
@RequiredArgsConstructor
class TemporaryCredentialsPersister {

	private final UserRepository userRepository;
	private final UserFinder userFinder;
	private final UserAdminGuard adminGuard;
	private final AssignableRoles assignableRoles;
	private final TemporaryPasswordGenerator passwordGenerator;
	private final PasswordHasher passwordHasher;
	private final RefreshTokenRepositoryPort refreshTokenRepository;
	private final AccountBlockedRepository accountBlockedRepository;

	@Transactional
	IssuedCredentials createUser(CreateUserCommand command) {
		String username = Usernames.normalize(command.username());
		String email = requireEmail(command.email());
		String phone = PhoneNumbers.normalize(command.phone());

		if (userRepository.existsByUsername(username)) {
			throw new AuthException(AuthError.USERNAME_ALREADY_IN_USE);
		}
		if (userRepository.existsByEmail(email)) {
			throw new AuthException(AuthError.EMAIL_ALREADY_IN_USE);
		}
		if (userRepository.existsByPhone(phone)) {
			throw new DuplicatePhoneException("Ya existe un usuario con ese teléfono");
		}
		var roles = assignableRoles.resolve(command.roleIds());

		String temporaryPassword = passwordGenerator.generate();
		User saved = userRepository.save(User.builder()
				.username(username)
				.email(email)
				.name(blankToNull(command.name()))
				.phone(phone)
				.password(passwordHasher.hash(temporaryPassword))
				.mustChangePassword(true)
				.active(true)
				.roles(new HashSet<>(roles))
				.build());
		return new IssuedCredentials(saved, temporaryPassword);
	}

	@Transactional
	IssuedCredentials resetPassword(Long actorId, Long userId) {
		adminGuard.ensureNotSelf(actorId, userId, "Usa el cambio de contraseña para tu propia cuenta");
		User user = userFinder.getOrThrow(userId);
		if (user.getPhone() == null) {
			throw new InvalidFieldException("phone", "the user has no phone number; set one before resetting the password");
		}

		String temporaryPassword = passwordGenerator.generate();
		user.setPassword(passwordHasher.hash(temporaryPassword));
		user.setMustChangePassword(true);
		User saved = userRepository.save(user);

		// La contraseña anterior deja de valer: se cierran sus sesiones y se limpia un posible bloqueo por intentos.
		refreshTokenRepository.revokeAllByUserId(userId);
		accountBlockedRepository.findByUserId(userId).ifPresent(accountBlockedRepository::resetAttempts);
		return new IssuedCredentials(saved, temporaryPassword);
	}

	private String requireEmail(String rawEmail) {
		if (rawEmail == null || rawEmail.isBlank()) {
			throw new InvalidFieldException("email", "must not be null or blank");
		}
		return rawEmail.strip();
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
