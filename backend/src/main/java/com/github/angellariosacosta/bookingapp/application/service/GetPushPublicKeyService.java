package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.port.in.GetPushPublicKeyUseCase;
import com.github.angellariosacosta.bookingapp.domain.exception.PushNotConfiguredException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPushPublicKeyService implements GetPushPublicKeyUseCase {

	static final String NOT_CONFIGURED_MESSAGE = "Las notificaciones no están disponibles en este momento";

	private final PushPolicy policy;

	@Override
	public String getPublicKey() {
		if (!policy.configured()) {
			throw new PushNotConfiguredException(NOT_CONFIGURED_MESSAGE);
		}
		return policy.vapidPublicKey();
	}
}
