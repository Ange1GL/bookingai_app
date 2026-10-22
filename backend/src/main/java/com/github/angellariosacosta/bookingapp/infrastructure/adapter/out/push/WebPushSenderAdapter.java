package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.push;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.WebPushSenderPort;
import com.github.angellariosacosta.bookingapp.application.result.PushDeliveryStatus;
import com.github.angellariosacosta.bookingapp.application.result.PushMessage;
import com.github.angellariosacosta.bookingapp.domain.model.PushSubscription;
import com.github.angellariosacosta.bookingapp.infrastructure.config.PushProperties;

import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import tools.jackson.databind.json.JsonMapper;

/**
 * Envía Web Push (RFC 8030/8291/8292) con la librería web-push. TTL corto y urgencia alta: un recordatorio
 * de "en 5 min" que llega tarde ya no sirve, así que el servicio push debe descartarlo en vez de guardarlo.
 * Del proveedor solo se registra el código HTTP: ni el endpoint ni las claves del dispositivo.
 */
@Slf4j
@Component
public class WebPushSenderAdapter implements WebPushSenderPort {

	private static final int TTL_SECONDS = 300;
	private static final int SEND_TIMEOUT_SECONDS = 10;
	private static final int STATUS_GONE = 410;
	private static final int STATUS_NOT_FOUND = 404;

	private final PushService pushService;
	private final JsonMapper jsonMapper;

	public WebPushSenderAdapter(PushProperties properties, JsonMapper jsonMapper) throws GeneralSecurityException {
		this.jsonMapper = jsonMapper;
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		this.pushService = properties.isConfigured()
				? new PushService(properties.getVapidPublicKey(), properties.getVapidPrivateKey(), properties.getSubject())
				: null;
	}

	@Override
	public PushDeliveryStatus send(PushSubscription subscription, PushMessage message) {
		if (pushService == null) {
			return PushDeliveryStatus.FAILED;
		}
		try {
			Notification notification = Notification.builder()
					.endpoint(subscription.endpoint())
					.userPublicKey(subscription.p256dh())
					.userAuth(subscription.auth())
					.payload(jsonMapper.writeValueAsBytes(message))
					.ttl(TTL_SECONDS)
					.urgency(Urgency.HIGH)
					.build();
			// Con límite de tiempo: el job comparte el hilo del scheduler y un servicio push colgado lo bloquearía.
			HttpResponse response = pushService.sendAsync(notification).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			return toStatus(response.getStatusLine().getStatusCode());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return PushDeliveryStatus.FAILED;
		} catch (Exception ex) {
			// El puerto promete no lanzar: claves inválidas, timeout, red o cifrado fallido son un envío fallido.
			log.error("Fallo al enviar la push: {}", ex.getClass().getSimpleName());
			return PushDeliveryStatus.FAILED;
		}
	}

	private static PushDeliveryStatus toStatus(int httpStatus) {
		if (httpStatus == STATUS_GONE || httpStatus == STATUS_NOT_FOUND) {
			return PushDeliveryStatus.GONE;
		}
		if (httpStatus >= 200 && httpStatus < 300) {
			return PushDeliveryStatus.SENT;
		}
		log.warn("El servicio push rechazó el mensaje: HTTP {}", httpStatus);
		return PushDeliveryStatus.FAILED;
	}
}
