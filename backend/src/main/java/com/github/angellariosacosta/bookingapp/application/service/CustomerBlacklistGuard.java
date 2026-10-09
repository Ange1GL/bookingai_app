package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerBlacklistedException;

import lombok.RequiredArgsConstructor;

/** Único punto que impide reservar a un cliente en lista negra (REST y herramientas de AI). */
@Component
@RequiredArgsConstructor
public class CustomerBlacklistGuard {

	private final CustomerBlacklistRepositoryPort blacklistRepository;

	public void ensureNotBlacklisted(Long customerId, Long userId) {
		if (blacklistRepository.existsByCustomerId(customerId, userId)) {
			throw new CustomerBlacklistedException(
					"Customer %d is blacklisted and cannot book appointments".formatted(customerId));
		}
	}
}
