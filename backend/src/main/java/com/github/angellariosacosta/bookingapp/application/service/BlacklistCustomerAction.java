package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.BlacklistResult;
import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;

import lombok.RequiredArgsConstructor;

/**
 * Paso común a "bloquear a mano" y "bloquear por umbral de inasistencias": guarda la entrada y
 * cancela las citas RESERVED futuras del cliente. Debe invocarse dentro de una transacción.
 */
@Component
@RequiredArgsConstructor
class BlacklistCustomerAction {

	private final CustomerBlacklistRepositoryPort blacklistRepository;
	private final AppointmentRepositoryPort appointmentRepository;
	private final Clock clock;

	BlacklistResult apply(CustomerBlacklist entry) {
		CustomerBlacklist saved = blacklistRepository.save(entry);
		int cancelled = appointmentRepository.cancelReservedFrom(
				saved.getCustomerId(), saved.getUserId(), LocalDateTime.now(clock));
		return new BlacklistResult(saved.getCustomerId(), saved.getReason(), saved.getSource(), cancelled);
	}
}
