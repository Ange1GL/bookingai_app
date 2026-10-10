package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.BlacklistCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BlacklistCustomerUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlacklistCustomerService implements BlacklistCustomerUseCase {

	private final CustomerRepositoryPort customerRepository;
	private final CustomerBlacklistRepositoryPort blacklistRepository;
	private final BlacklistCustomerAction blacklistAction;
	private final Clock clock;

	// Idempotente: si el cliente ya estaba en la lista no se duplica ni se cambia su motivo.
	@Override
	@Transactional
	public void blacklist(BlacklistCustomerCommand command) {
		customerRepository.findById(command.customerId(), command.userId())
				.orElseThrow(() -> new CustomerNotFoundException(
						"Customer not found with id: " + command.customerId()));
		if (blacklistRepository.existsByCustomerId(command.customerId(), command.userId())) {
			return;
		}
		blacklistAction.apply(CustomerBlacklist.create(
				command.customerId(), command.userId(), command.reason(), clock.instant()));
	}
}
