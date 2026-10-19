package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.ChangePasswordCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.ChangePasswordUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import com.github.angellariosacosta.bookingapp.domain.shared.PasswordPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordService implements ChangePasswordUseCase {

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;
	private final AccountBlockedRepository accountBlockedRepository;
	private final FailedLoginAttempts failedLoginAttempts;
	private final RefreshTokenRepositoryPort refreshTokenRepository;
	private final AccessTokenRevoker accessTokenRevoker;
	private final AuthTokenIssuer authTokenIssuer;
	private final Clock clock;

	// noRollbackFor: si la contraseña actual es incorrecta se lanza AuthException, pero el intento fallido que se
	// acaba de contar (bloqueo por fuerza bruta) debe persistir; con el rollback por defecto se perdería.
	@Override
	@Transactional(noRollbackFor = AuthException.class)
	public AuthTokenResult changePassword(ChangePasswordCommand command) {
		User user = userRepository.findById(command.userId())
				.orElseThrow(() -> new AuthException(AuthError.INVALID_CREDENTIALS));

		Optional<AccountBlocked> accountBlocked = accountBlockedRepository.findByUserId(user.getId());
		accountBlocked.ifPresent(AccountBlocked::ensureNotBlocked);

		if (!passwordHasher.matches(command.currentPassword(), user.getPassword())) {
			log.warn("Password change failed user={} reason=invalid_current_password", user.getUsername());
			failedLoginAttempts.register(user.getId());
			throw new AuthException(AuthError.INVALID_CURRENT_PASSWORD);
		}

		PasswordPolicy.validate(command.newPassword());
		if (passwordHasher.matches(command.newPassword(), user.getPassword())) {
			throw new InvalidFieldException("newPassword", "must be different from the current password");
		}

		user.setPassword(passwordHasher.hash(command.newPassword()));
		user.setMustChangePassword(false);
		user.setPasswordChangedAt(Instant.now(clock));
		User saved = userRepository.save(user);

		// Cualquier sesión abierta con la contraseña anterior (o con la temporal) deja de valer; se abre una nueva.
		refreshTokenRepository.revokeAllByUserId(saved.getId());
		accessTokenRevoker.revoke(command.currentAccessToken());
		accountBlocked.ifPresent(accountBlockedRepository::resetAttempts);

		log.info("Password changed user={}", saved.getUsername());
		return authTokenIssuer.issue(saved, command.userAgent(), command.ipAddress()).result();
	}
}
