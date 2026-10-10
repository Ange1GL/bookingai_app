package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;

import lombok.RequiredArgsConstructor;

/**
 * Manda al cliente a la lista negra y cancela sus citas RESERVED futuras. Debe invocarse dentro de
 * una transacción. Devuelve cuántas citas se cancelaron.
 */
@Component
@RequiredArgsConstructor
class BlacklistCustomerAction {

	private final CustomerBlacklistRepositoryPort blacklistRepository;
	private final AppointmentRepositoryPort appointmentRepository;
	private final Clock clock;

	int apply(CustomerBlacklist entry) {
		CustomerBlacklist saved = blacklistRepository.save(entry);
		return appointmentRepository.cancelReservedFrom(
				saved.getCustomerId(), saved.getUserId(), LocalDateTime.now(clock));
	}
}
