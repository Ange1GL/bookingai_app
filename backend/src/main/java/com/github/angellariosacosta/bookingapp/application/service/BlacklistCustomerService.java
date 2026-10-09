package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.BlacklistCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BlacklistCustomerUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.BlacklistResult;
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

	@Override
	@Transactional
	public BlacklistResult blacklist(BlacklistCustomerCommand command) {
		customerRepository.findById(command.customerId(), command.userId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + command.customerId()));

		// Idempotente: si ya estaba bloqueado se conserva la entrada original y no se cancela nada más.
		return blacklistRepository.findByCustomerId(command.customerId(), command.userId())
				.map(existing -> new BlacklistResult(
						existing.getCustomerId(), existing.getReason(), existing.getSource(), 0))
				.orElseGet(() -> blacklistAction.apply(CustomerBlacklist.manual(
						command.customerId(), command.userId(), command.reason(), clock.instant())));
	}
}
