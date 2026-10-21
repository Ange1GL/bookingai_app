package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;

import lombok.RequiredArgsConstructor;

/** Sin {@code @Transactional} a propósito: persistir y entregar son pasos separados (ver OneTimeCodeIssuer). */
@Service
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

	private final OneTimeCodeIssuer issuer;
	private final OneTimeCodeDelivery delivery;

	@Override
	public IssuedCodeResult create(CreateUserCommand command) {
		return delivery.deliver(issuer.issueForNewUser(command));
	}
}
