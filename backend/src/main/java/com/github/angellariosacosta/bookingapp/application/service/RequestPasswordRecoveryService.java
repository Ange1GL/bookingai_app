package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.RequestPasswordRecoveryUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordRecoveryRequestRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestPasswordRecoveryService implements RequestPasswordRecoveryUseCase {

	private final UserRepository userRepository;
	private final PasswordRecoveryRequestRepositoryPort requestRepository;
	private final Clock clock;

	/** Un username inexistente, deshabilitado o sin teléfono se ignora en silencio: el cliente no puede distinguirlo. */
	@Override
	@Transactional
	public void request(String username) {
		if (username == null || username.isBlank()) {
			return;
		}
		userRepository.findByUsername(username.strip())
				.filter(user -> user.isActive() && user.getPhone() != null)
				.ifPresent(user -> {
					requestRepository.registerPending(user.getId(), Instant.now(clock));
					log.info("Solicitud de recuperación de contraseña registrada userId={}", user.getId());
				});
	}
}
