package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.port.in.SendTestPushUseCase;
import com.github.angellariosacosta.bookingapp.application.result.PushMessage;
import com.github.angellariosacosta.bookingapp.domain.exception.PushNotConfiguredException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SendTestPushService implements SendTestPushUseCase {

	static final PushMessage TEST_MESSAGE = new PushMessage(
			"Recordatorios activados", "Así te avisaremos antes de cada cita.", "/appointments", "push-test");

	private final PushNotifier notifier;
	private final PushPolicy policy;

	@Override
	public int sendTest(Long userId) {
		if (!policy.configured()) {
			throw new PushNotConfiguredException(GetPushPublicKeyService.NOT_CONFIGURED_MESSAGE);
		}
		return notifier.sendToUser(userId, TEST_MESSAGE);
	}
}
