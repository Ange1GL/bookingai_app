package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.PushSubscriptionRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.WebPushSenderPort;
import com.github.angellariosacosta.bookingapp.application.result.PushDeliveryStatus;
import com.github.angellariosacosta.bookingapp.application.result.PushMessage;
import com.github.angellariosacosta.bookingapp.domain.model.PushSubscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Envía un mensaje a todos los dispositivos de un usuario y limpia las suscripciones que ya no existen. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotifier {

	private final PushSubscriptionRepositoryPort subscriptions;
	private final WebPushSenderPort sender;

	/** Devuelve a cuántos dispositivos llegó el mensaje. */
	public int sendToUser(Long userId, PushMessage message) {
		List<PushSubscription> devices = subscriptions.findByUserId(userId);
		int delivered = 0;
		for (PushSubscription device : devices) {
			PushDeliveryStatus status = sender.send(device, message);
			switch (status) {
				case SENT -> delivered++;
				case GONE -> subscriptions.deleteByEndpoint(device.endpoint());
				case FAILED -> log.warn("Push no entregada: userId={} subscriptionId={}", userId, device.id());
			}
		}
		return delivered;
	}
}
