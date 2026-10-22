package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.UnsubscribeFromPushCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.UnsubscribeFromPushUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PushSubscriptionRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UnsubscribeFromPushService implements UnsubscribeFromPushUseCase {

	private final PushSubscriptionRepositoryPort subscriptions;

	// Idempotente y acotado al usuario: nadie puede borrar el dispositivo de otro.
	@Override
	@Transactional
	public void unsubscribe(UnsubscribeFromPushCommand command) {
		subscriptions.deleteByUserIdAndEndpoint(command.userId(), command.endpoint());
	}
}
