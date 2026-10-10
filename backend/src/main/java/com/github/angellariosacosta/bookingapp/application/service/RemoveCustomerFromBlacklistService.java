package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.RemoveCustomerFromBlacklistCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.RemoveCustomerFromBlacklistUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveCustomerFromBlacklistService implements RemoveCustomerFromBlacklistUseCase {

	private final CustomerBlacklistRepositoryPort blacklistRepository;

	// No restaura las citas canceladas al bloquear.
	@Override
	@Transactional
	public void remove(RemoveCustomerFromBlacklistCommand command) {
		if (!blacklistRepository.deleteByCustomerId(command.customerId(), command.userId())) {
			throw new CustomerNotFoundException(
					"Customer %d is not in the blacklist".formatted(command.customerId()));
		}
	}
}
