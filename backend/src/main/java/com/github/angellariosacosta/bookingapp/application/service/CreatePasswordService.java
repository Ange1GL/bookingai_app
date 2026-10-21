package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreatePasswordCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreatePasswordUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.VerificationTicketPort;
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
	private final OneTimeCodeChecker codeChecker;
	private final VerificationTicketPort verificationTickets;

	CreatePasswordService(UserRepository userRepository, OneTimeCodeRepositoryPort oneTimeCodeRepository,
			PasswordHasher passwordHasher, RefreshTokenRepositoryPort refreshTokenRepository,
			AccountBlockedRepository accountBlockedRepository, AuthTokenIssuer authTokenIssuer, Clock clock, OneTimeCodeChecker codeChecker,
			VerificationTicketPort verificationTickets) {
		this.userRepository = userRepository;
		this.oneTimeCodeRepository = oneTimeCodeRepository;
		this.passwordHasher = passwordHasher;
		this.refreshTokenRepository = refreshTokenRepository;
		this.accountBlockedRepository = accountBlockedRepository;
		this.authTokenIssuer = authTokenIssuer;
		this.clock = clock;
		this.codeChecker = codeChecker;
		this.verificationTickets = verificationTickets;
	}

	// noRollbackFor: un código incorrecto lanza la excepción tras contar el intento fallido; con el rollback por
	// defecto ese conteo se perdería y el límite de intentos no serviría.
	@Override
	@Transactional(noRollbackFor = InvalidOneTimeCodeException.class)
	public AuthTokenResult createPassword(CreatePasswordCommand command) {
		// Antes de tocar el código: una contraseña que no cumple la política no debe gastar un intento.
		PasswordPolicy.validate(command.newPassword());

		Instant now = Instant.now(clock);

		OneTimeCodeChecker.Verified verified = codeChecker.check(command.username(), command.code(), now);
		User user = verified.user();
		OneTimeCode code = verified.code();

		// El paso 1 (verify-code) es obligatorio: sin su comprobante firmado no se canjea el código aunque sea correcto.
		if (!verificationTickets.isValid(command.verificationToken(), user.getId(), code.getId(), now)) {
			log.warn("Missing or invalid verification ticket user={}", user.getUsername());
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
