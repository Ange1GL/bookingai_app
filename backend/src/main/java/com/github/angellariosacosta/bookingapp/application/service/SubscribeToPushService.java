package com.github.angellariosacosta.bookingapp.application.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.SubscribeToPushCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.SubscribeToPushUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PushSubscriptionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidPushSubscriptionException;
import com.github.angellariosacosta.bookingapp.domain.model.PushSubscription;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscribeToPushService implements SubscribeToPushUseCase {

	static final String INVALID_ENDPOINT_MESSAGE = "El dispositivo no es compatible con las notificaciones";

	private static final String HTTPS = "https";
	private static final int DEFAULT_HTTPS_PORT = 443;

	private final PushSubscriptionRepositoryPort subscriptions;
	private final PushPolicy policy;

	@Override
	@Transactional
	public void subscribe(SubscribeToPushCommand command) {
		requireKnownPushService(command.endpoint());
		subscriptions.upsert(new PushSubscription(null, command.userId(), command.endpoint(), command.p256dh(),
				command.auth(), command.userAgent()));
	}

	private void requireKnownPushService(String endpoint) {
		URI uri;
		try {
			uri = new URI(endpoint);
		} catch (URISyntaxException ex) {
			throw new InvalidPushSubscriptionException(INVALID_ENDPOINT_MESSAGE);
		}
		boolean safeShape = HTTPS.equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
				&& (uri.getPort() == -1 || uri.getPort() == DEFAULT_HTTPS_PORT);
		if (!safeShape || !isAllowedHost(uri.getHost())) {
			throw new InvalidPushSubscriptionException(INVALID_ENDPOINT_MESSAGE);
		}
	}

	// El sufijo debe coincidir en el límite de un punto: "evil-fcm.googleapis.com" no es "fcm.googleapis.com".
	private boolean isAllowedHost(String host) {
		if (host == null) {
			return false;
		}
		String normalized = host.toLowerCase(Locale.ROOT);
		return policy.allowedEndpointHostSuffixes().stream()
				.anyMatch(suffix -> normalized.equals(suffix) || normalized.endsWith("." + suffix));
	}
}
