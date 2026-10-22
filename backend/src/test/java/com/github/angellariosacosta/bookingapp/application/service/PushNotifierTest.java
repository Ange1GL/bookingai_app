package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.port.out.PushSubscriptionRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.WebPushSenderPort;
import com.github.angellariosacosta.bookingapp.application.result.PushDeliveryStatus;
import com.github.angellariosacosta.bookingapp.application.result.PushMessage;
import com.github.angellariosacosta.bookingapp.domain.model.PushSubscription;

@ExtendWith(MockitoExtension.class)
class PushNotifierTest {

	private static final Long USER_ID = 7L;
	private static final PushMessage MESSAGE = new PushMessage("t", "b", "/appointments", "tag");
	private static final PushSubscription PHONE = new PushSubscription(1L, USER_ID, "https://fcm.googleapis.com/phone", "k", "a", "ua");
	private static final PushSubscription LAPTOP = new PushSubscription(2L, USER_ID, "https://fcm.googleapis.com/laptop", "k", "a", "ua");

	@Mock
	private PushSubscriptionRepositoryPort subscriptions;

	@Mock
	private WebPushSenderPort sender;

	@InjectMocks
	private PushNotifier notifier;

	@Test
	@DisplayName("envía a todos los dispositivos del usuario y cuenta los entregados")
	void sendsToEveryDevice() {
		when(subscriptions.findByUserId(USER_ID)).thenReturn(List.of(PHONE, LAPTOP));
		when(sender.send(any(), any())).thenReturn(PushDeliveryStatus.SENT);

		assertEquals(2, notifier.sendToUser(USER_ID, MESSAGE));
	}

	@Test
	@DisplayName("borra el dispositivo cuando el servicio push responde que ya no existe")
	void removesGoneSubscriptions() {
		when(subscriptions.findByUserId(USER_ID)).thenReturn(List.of(PHONE, LAPTOP));
		when(sender.send(PHONE, MESSAGE)).thenReturn(PushDeliveryStatus.GONE);
		when(sender.send(LAPTOP, MESSAGE)).thenReturn(PushDeliveryStatus.SENT);

		assertEquals(1, notifier.sendToUser(USER_ID, MESSAGE));

		verify(subscriptions).deleteByEndpoint(PHONE.endpoint());
		verify(subscriptions, never()).deleteByEndpoint(LAPTOP.endpoint());
	}

	@Test
	@DisplayName("un fallo transitorio no borra el dispositivo ni frena a los demás")
	void failureKeepsSubscription() {
		when(subscriptions.findByUserId(USER_ID)).thenReturn(List.of(PHONE, LAPTOP));
		when(sender.send(PHONE, MESSAGE)).thenReturn(PushDeliveryStatus.FAILED);
		when(sender.send(LAPTOP, MESSAGE)).thenReturn(PushDeliveryStatus.SENT);

		assertEquals(1, notifier.sendToUser(USER_ID, MESSAGE));

		verify(subscriptions, never()).deleteByEndpoint(any());
	}

	@Test
	@DisplayName("sin dispositivos no entrega nada")
	void noDevices() {
		when(subscriptions.findByUserId(USER_ID)).thenReturn(List.of());

		assertEquals(0, notifier.sendToUser(USER_ID, MESSAGE));
	}
}
