package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.port.in.ResetUserPasswordUseCase;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCredentialsResult;

import lombok.RequiredArgsConstructor;

/** Sin {@code @Transactional} a propósito: persistir y entregar son pasos separados (ver TemporaryCredentialsPersister). */
@Service
@RequiredArgsConstructor
public class ResetUserPasswordService implements ResetUserPasswordUseCase {

	private final TemporaryCredentialsPersister persister;
	private final CredentialsDelivery delivery;

	@Override
	public IssuedCredentialsResult reset(Long actorId, Long userId) {
		return delivery.deliver(persister.resetPassword(actorId, userId));
	}
}
