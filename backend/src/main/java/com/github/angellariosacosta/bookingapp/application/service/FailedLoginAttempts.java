package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;

import lombok.RequiredArgsConstructor;

/** Cuenta los intentos fallidos de contraseña de un usuario (login y cambio de contraseña) y aplica el bloqueo. */
@Component
@RequiredArgsConstructor
class FailedLoginAttempts {

	private final AccountBlockedRepository accountBlockedRepository;

	// Relee con lock pesimista (en vez de reusar el Optional cargado al inicio del flujo)
	// para que el read-modify-write del contador sea atómico frente a intentos fallidos
	// concurrentes del mismo usuario.
	void register(Long userId) {
		accountBlockedRepository.findByUserIdForUpdate(userId).ifPresentOrElse(
				blocked -> {
					blocked.incrementNumberOfAttempts();
					accountBlockedRepository.updateNumberOfAttempts(blocked);
					if (blocked.getEndExpiratedAt() != null) {
						accountBlockedRepository.updateExpiratedAt(blocked);
					}
				},
				() -> accountBlockedRepository.save(AccountBlocked.create(userId))
		);
	}
}
