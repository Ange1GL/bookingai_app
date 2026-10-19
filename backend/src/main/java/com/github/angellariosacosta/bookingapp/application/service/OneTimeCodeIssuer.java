package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeGenerator;
import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePhoneException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCodePurpose;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneNumbers;
import com.github.angellariosacosta.bookingapp.domain.shared.Usernames;

/**
 * Parte transaccional de emitir un código de un solo uso. Se separa de la entrega por WhatsApp para que el mensaje
 * solo salga cuando la transacción ya hizo commit (no se envía un código de un usuario que no llegó a guardarse).
 */
@Component
class OneTimeCodeIssuer {

	private final UserRepository userRepository;
	private final UserFinder userFinder;
	private final UserAdminGuard adminGuard;
	private final AssignableRoles assignableRoles;
	private final OneTimeCodeGenerator codeGenerator;
	private final PasswordHasher passwordHasher;
	private final OneTimeCodeRepositoryPort oneTimeCodeRepository;
	private final RefreshTokenRepositoryPort refreshTokenRepository;
	private final AccountBlockedRepository accountBlockedRepository;
	private final Clock clock;
	private final Duration ttl;

	OneTimeCodeIssuer(UserRepository userRepository, UserFinder userFinder, UserAdminGuard adminGuard,
			AssignableRoles assignableRoles, OneTimeCodeGenerator codeGenerator, PasswordHasher passwordHasher,
			OneTimeCodeRepositoryPort oneTimeCodeRepository, RefreshTokenRepositoryPort refreshTokenRepository,
			AccountBlockedRepository accountBlockedRepository, Clock clock,
			@Value("${security.otp.ttl-minutes}") long ttlMinutes) {
		this.userRepository = userRepository;
		this.userFinder = userFinder;
		this.adminGuard = adminGuard;
		this.assignableRoles = assignableRoles;
		this.codeGenerator = codeGenerator;
		this.passwordHasher = passwordHasher;
		this.oneTimeCodeRepository = oneTimeCodeRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.accountBlockedRepository = accountBlockedRepository;
		this.clock = clock;
		this.ttl = Duration.ofMinutes(ttlMinutes);
	}

	/** Crea el usuario SIN contraseña (queda pendiente de crearla) y le emite su código de activación. */
	@Transactional
	IssuedCode issueForNewUser(CreateUserCommand command) {
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

		User saved = userRepository.save(User.builder()
				.username(username)
				.email(email)
				.name(blankToNull(command.name()))
				.phone(phone)
				.active(true)
				.roles(new HashSet<>(roles))
				.build());
		return issue(saved, OneTimeCodePurpose.ACCOUNT_ACTIVATION);
	}

	/**
	 * Invalida la contraseña del usuario y le emite un código para crear otra. Si el usuario aún no tenía contraseña
	 * (cuenta sin activar) el propósito es la activación; si la tenía, es un reinicio.
	 */
	@Transactional
	IssuedCode issueForExistingUser(Long actorId, Long userId) {
		adminGuard.ensureNotSelf(actorId, userId, "Usa el cambio de contraseña para tu propia cuenta");
		User user = userFinder.getOrThrow(userId);
		if (user.getPhone() == null) {
			throw new InvalidFieldException("phone", "the user has no phone number; set one before sending a code");
		}

		OneTimeCodePurpose purpose = user.getPassword() == null
				? OneTimeCodePurpose.ACCOUNT_ACTIVATION
				: OneTimeCodePurpose.PASSWORD_RESET;
		user.setPassword(null);
		User saved = userRepository.save(user);

		// La contraseña anterior deja de valer: se cierran sus sesiones y se limpia un posible bloqueo por intentos.
		refreshTokenRepository.revokeAllByUserId(userId);
		accountBlockedRepository.findByUserId(userId).ifPresent(accountBlockedRepository::resetAttempts);
		return issue(saved, purpose);
	}

	private IssuedCode issue(User user, OneTimeCodePurpose purpose) {
		Instant now = Instant.now(clock);
		oneTimeCodeRepository.consumePending(user.getId(), now);

		String code = codeGenerator.generate();
		oneTimeCodeRepository.save(OneTimeCode.issue(user.getId(), purpose, passwordHasher.hash(code), now, ttl));
		return new IssuedCode(user, code);
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
