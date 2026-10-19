package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.port.in.ResetUserPasswordUseCase;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;

import lombok.RequiredArgsConstructor;

/** Sin {@code @Transactional} a propósito: persistir y entregar son pasos separados (ver OneTimeCodeIssuer). */
@Service
@RequiredArgsConstructor
public class ResetUserPasswordService implements ResetUserPasswordUseCase {

	private final OneTimeCodeIssuer issuer;
	private final OneTimeCodeDelivery delivery;

	@Override
	public IssuedCodeResult reset(Long actorId, Long userId) {
		return delivery.deliver(issuer.issueForExistingUser(actorId, userId));
	}
}
