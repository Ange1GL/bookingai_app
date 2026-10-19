package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCredentialsResult;

import lombok.RequiredArgsConstructor;

/** Sin {@code @Transactional} a propósito: persistir y entregar son pasos separados (ver TemporaryCredentialsPersister). */
@Service
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

	private final TemporaryCredentialsPersister persister;
	private final CredentialsDelivery delivery;

	@Override
	public IssuedCredentialsResult create(CreateUserCommand command) {
		return delivery.deliver(persister.createUser(command));
	}
}
