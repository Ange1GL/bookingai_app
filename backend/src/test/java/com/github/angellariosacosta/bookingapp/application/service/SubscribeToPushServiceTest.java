package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.command.SubscribeToPushCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.PushSubscriptionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidPushSubscriptionException;

@ExtendWith(MockitoExtension.class)
class SubscribeToPushServiceTest {

	private static final Long USER_ID = 7L;

	@Mock
	private PushSubscriptionRepositoryPort subscriptions;

	private SubscribeToPushService service;

	@BeforeEach
	void setUp() {
		PushPolicy policy = new PushPolicy("key", 5,
				List.of("fcm.googleapis.com", "push.services.mozilla.com", "push.apple.com", "notify.windows.com"));
		service = new SubscribeToPushService(subscriptions, policy);
	}

	private SubscribeToPushCommand command(String endpoint) {
		return new SubscribeToPushCommand(USER_ID, endpoint, "p256dh", "auth", "Mozilla/5.0");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"https://fcm.googleapis.com/fcm/send/abc",
			"https://updates.push.services.mozilla.com/wpush/v2/abc",
			"https://web.push.apple.com/QAbc",
			"https://wns2-par02p.notify.windows.com/w/?token=abc",
			"https://FCM.GOOGLEAPIS.COM:443/fcm/send/abc"
	})
	@DisplayName("acepta los servicios push de los navegadores")
	void acceptsKnownPushServices(String endpoint) {
		service.subscribe(command(endpoint));

		verify(subscriptions).upsert(any());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://fcm.googleapis.com/fcm/send/abc",
			"https://localhost/hook",
			"https://169.254.169.254/latest/meta-data",
			"https://10.0.0.5/internal",
			"https://evil-fcm.googleapis.com/x",
			"https://fcm.googleapis.com.evil.com/x",
			"https://user@fcm.googleapis.com/x",
			"https://fcm.googleapis.com:8443/x",
			"ftp://fcm.googleapis.com/x",
			"not a url"
	})
	@DisplayName("rechaza endpoints que no son un servicio push conocido (evita SSRF)")
	void rejectsUnknownEndpoints(String endpoint) {
		assertThrows(InvalidPushSubscriptionException.class, () -> service.subscribe(command(endpoint)));

		verify(subscriptions, never()).upsert(any());
	}
}
