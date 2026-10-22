package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidOneTimeCodeException;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * Comprueba usuario + código de un solo uso contando los intentos fallidos. Lo comparten la verificación del paso 1 y
 * el canje final, así ambos aplican exactamente el mismo límite. Debe llamarse dentro de una transacción que NO haga
 * rollback por {@link InvalidOneTimeCodeException}, o el conteo del intento fallido se perdería.
 */
@Slf4j
@Component
class OneTimeCodeChecker {

	record Verified(User user, OneTimeCode code) {
	}

	private final UserRepository userRepository;
	private final OneTimeCodeRepositoryPort oneTimeCodeRepository;
	private final PasswordHasher passwordHasher;
	// Hash de relleno: se compara cuando el usuario/código no existe para que la respuesta tarde lo mismo y no
	// revele qué usernames existen.
	private final String decoyHash;

	OneTimeCodeChecker(UserRepository userRepository, OneTimeCodeRepositoryPort oneTimeCodeRepository,
			PasswordHasher passwordHasher) {
		this.userRepository = userRepository;
		this.oneTimeCodeRepository = oneTimeCodeRepository;
		this.passwordHasher = passwordHasher;
		this.decoyHash = passwordHasher.hash(UUID.randomUUID().toString());
	}

	Verified check(String username, String rawCode, Instant now) {
		Optional<User> found = userRepository.findByUsername(username).filter(User::isActive);
		Optional<OneTimeCode> pending = found.flatMap(user -> oneTimeCodeRepository.findPendingByUserIdForUpdate(user.getId()));

		if (pending.isEmpty() || !pending.get().isUsable(now)) {
			passwordHasher.matches(rawCode, decoyHash);
			throw new InvalidOneTimeCodeException();
		}
		User user = found.orElseThrow();
		OneTimeCode code = pending.get();

		if (!passwordHasher.matches(rawCode, code.getCodeHash())) {
			code.registerFailure(now);
			oneTimeCodeRepository.save(code);
			log.warn("Invalid one-time code user={} failedAttempts={}", user.getUsername(), code.getFailedAttempts());
			throw new InvalidOneTimeCodeException();
		}
		return new Verified(user, code);
	}
}
