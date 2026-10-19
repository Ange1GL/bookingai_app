package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreatePasswordCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreatePasswordUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidOneTimeCodeException;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.PasswordPolicy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CreatePasswordService implements CreatePasswordUseCase {

	private final UserRepository userRepository;
	private final OneTimeCodeRepositoryPort oneTimeCodeRepository;
	private final PasswordHasher passwordHasher;
	private final RefreshTokenRepositoryPort refreshTokenRepository;
	private final AccountBlockedRepository accountBlockedRepository;
	private final AuthTokenIssuer authTokenIssuer;
	private final Clock clock;
	// Hash de relleno: se compara cuando el usuario/código no existe para que la respuesta tarde lo mismo y no
	// revele qué usernames existen.
	private final String decoyHash;

	CreatePasswordService(UserRepository userRepository, OneTimeCodeRepositoryPort oneTimeCodeRepository,
			PasswordHasher passwordHasher, RefreshTokenRepositoryPort refreshTokenRepository,
			AccountBlockedRepository accountBlockedRepository, AuthTokenIssuer authTokenIssuer, Clock clock) {
		this.userRepository = userRepository;
		this.oneTimeCodeRepository = oneTimeCodeRepository;
		this.passwordHasher = passwordHasher;
		this.refreshTokenRepository = refreshTokenRepository;
		this.accountBlockedRepository = accountBlockedRepository;
		this.authTokenIssuer = authTokenIssuer;
		this.clock = clock;
		this.decoyHash = passwordHasher.hash(UUID.randomUUID().toString());
	}

	// noRollbackFor: un código incorrecto lanza la excepción tras contar el intento fallido; con el rollback por
	// defecto ese conteo se perdería y el límite de intentos no serviría.
	@Override
	@Transactional(noRollbackFor = InvalidOneTimeCodeException.class)
	public AuthTokenResult createPassword(CreatePasswordCommand command) {
		// Antes de tocar el código: una contraseña que no cumple la política no debe gastar un intento.
		PasswordPolicy.validate(command.newPassword());

		Optional<User> found = userRepository.findByUsername(command.username()).filter(User::isActive);
		Optional<OneTimeCode> pending = found.flatMap(user -> oneTimeCodeRepository.findPendingByUserIdForUpdate(user.getId()));
		Instant now = Instant.now(clock);

		if (pending.isEmpty() || !pending.get().isUsable(now)) {
			passwordHasher.matches(command.code(), decoyHash);
			throw new InvalidOneTimeCodeException();
		}
		User user = found.orElseThrow();
		OneTimeCode code = pending.get();

		if (!passwordHasher.matches(command.code(), code.getCodeHash())) {
			code.registerFailure(now);
			oneTimeCodeRepository.save(code);
			log.warn("Invalid one-time code user={} failedAttempts={}", user.getUsername(), code.getFailedAttempts());
			throw new InvalidOneTimeCodeException();
		}

		code.consume(now);
		oneTimeCodeRepository.save(code);

		user.setPassword(passwordHasher.hash(command.newPassword()));
		user.setPasswordChangedAt(now);
		User saved = userRepository.save(user);

		// Sin sesiones previas y sin bloqueo por intentos: la cuenta arranca limpia con la contraseña nueva.
		refreshTokenRepository.revokeAllByUserId(saved.getId());
		accountBlockedRepository.findByUserId(saved.getId()).ifPresent(accountBlockedRepository::resetAttempts);

		log.info("Password created with one-time code user={} purpose={}", saved.getUsername(), code.getPurpose());
		return authTokenIssuer.issue(saved, command.userAgent(), command.ipAddress()).result();
	}
}
