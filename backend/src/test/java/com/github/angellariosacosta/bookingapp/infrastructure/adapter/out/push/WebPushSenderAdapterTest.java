package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.result.PushDeliveryStatus;
import com.github.angellariosacosta.bookingapp.application.result.PushMessage;
import com.github.angellariosacosta.bookingapp.domain.model.PushSubscription;
import com.github.angellariosacosta.bookingapp.infrastructure.config.PushProperties;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.json.JsonMapper;

/**
 * Prueba el envío completo (cifrado aes128gcm + firma VAPID + cabeceras) contra un "servicio push" local
 * que solo responde un código HTTP. El contenido cifrado solo lo puede leer el navegador del usuario.
 */
class WebPushSenderAdapterTest {

	private static final PushMessage MESSAGE = new PushMessage("En 5 min: Juan", "Corte · 4:30 p. m.", "/appointments", "appointment-1");
	private static final int EC_COORDINATE_BYTES = 32;

	private HttpServer pushService;
	private final AtomicReference<com.sun.net.httpserver.Headers> receivedHeaders = new AtomicReference<>();
	private final AtomicReference<byte[]> receivedBody = new AtomicReference<>();
	private volatile int responseStatus = 201;

	@BeforeEach
	void startPushService() throws Exception {
		pushService = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		pushService.createContext("/push", exchange -> {
			receivedHeaders.set(exchange.getRequestHeaders());
			receivedBody.set(exchange.getRequestBody().readAllBytes());
			exchange.sendResponseHeaders(responseStatus, -1);
			exchange.close();
		});
		pushService.start();
	}

	@AfterEach
	void stopPushService() {
		pushService.stop(0);
	}

	@Test
	@DisplayName("envía el mensaje cifrado con firma VAPID, TTL corto y urgencia alta")
	void sendsEncryptedSignedRequest() throws Exception {
		WebPushSenderAdapter adapter = adapter();

		PushDeliveryStatus status = adapter.send(subscription(), MESSAGE);

		assertEquals(PushDeliveryStatus.SENT, status);
		var headers = receivedHeaders.get();
		assertNotNull(headers);
		assertTrue(headers.getFirst("Authorization").startsWith("vapid t="), "firma VAPID");
		assertEquals("aes128gcm", headers.getFirst("Content-Encoding"));
		assertEquals("300", headers.getFirst("TTL"));
		assertEquals("high", headers.getFirst("Urgency"));
		assertTrue(receivedBody.get().length > 0);
		assertTrue(new String(receivedBody.get(), java.nio.charset.StandardCharsets.ISO_8859_1).indexOf("Juan") < 0,
				"el cuerpo viaja cifrado");
	}

	@Test
	@DisplayName("el contenido usa el formato del service worker de Angular, con la ruta a abrir al tocarla")
	void payloadMatchesAngularServiceWorkerFormat() throws Exception {
		String json = JsonMapper.builder().build().writeValueAsString(WebPushSenderAdapter.toPayload(MESSAGE));

		var notification = JsonMapper.builder().build().readTree(json).get("notification");
		assertEquals("En 5 min: Juan", notification.get("title").asString());
		assertEquals("appointment-1", notification.get("tag").asString());
		var click = notification.get("data").get("onActionClick").get("default");
		assertEquals("navigateLastFocusedOrOpen", click.get("operation").asString());
		assertEquals("/appointments", click.get("url").asString());
	}

	@Test
	@DisplayName("404 y 410 significan que el dispositivo ya no existe")
	void goneStatuses() throws Exception {
		WebPushSenderAdapter adapter = adapter();
		PushSubscription subscription = subscription();

		responseStatus = 410;
		assertEquals(PushDeliveryStatus.GONE, adapter.send(subscription, MESSAGE));
		responseStatus = 404;
		assertEquals(PushDeliveryStatus.GONE, adapter.send(subscription, MESSAGE));
	}

	@Test
	@DisplayName("otros errores del servicio push son un fallo reintentable, no borran el dispositivo")
	void serverErrorIsFailure() throws Exception {
		responseStatus = 503;

		assertEquals(PushDeliveryStatus.FAILED, adapter().send(subscription(), MESSAGE));
	}

	@Test
	@DisplayName("un servicio push inalcanzable devuelve FAILED sin lanzar")
	void unreachableServiceIsFailure() throws Exception {
		PushSubscription unreachable = new PushSubscription(1L, 7L, "http://127.0.0.1:1/push", p256dh(), auth(), "ua");

		assertEquals(PushDeliveryStatus.FAILED, adapter().send(unreachable, MESSAGE));
	}

	@Test
	@DisplayName("sin claves VAPID no envía nada")
	void notConfigured() throws Exception {
		WebPushSenderAdapter adapter = new WebPushSenderAdapter(new PushProperties(), JsonMapper.builder().build());

		assertEquals(PushDeliveryStatus.FAILED, adapter.send(subscription(), MESSAGE));
		assertEquals(null, receivedHeaders.get());
	}

	private PushSubscription subscription() throws Exception {
		return new PushSubscription(1L, 7L, "http://127.0.0.1:" + pushService.getAddress().getPort() + "/push", p256dh(), auth(), "ua");
	}

	private WebPushSenderAdapter adapter() throws Exception {
		KeyPair vapid = newKeyPair();
		PushProperties properties = new PushProperties();
		properties.setVapidPublicKey(encodePublic((ECPublicKey) vapid.getPublic()));
		properties.setVapidPrivateKey(b64(unsigned(((ECPrivateKey) vapid.getPrivate()).getS(), EC_COORDINATE_BYTES)));
		properties.setSubject("mailto:test@example.com");
		return new WebPushSenderAdapter(properties, JsonMapper.builder().build());
	}

	private static String p256dh() throws Exception {
		return encodePublic((ECPublicKey) newKeyPair().getPublic());
	}

	private static String auth() {
		byte[] auth = new byte[16];
		new SecureRandom().nextBytes(auth);
		return b64(auth);
	}

	private static KeyPair newKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp256r1"));
		return generator.generateKeyPair();
	}

	// Punto sin comprimir (0x04 || X || Y), el formato que usan los navegadores y VAPID.
	private static String encodePublic(ECPublicKey key) {
		byte[] x = unsigned(key.getW().getAffineX(), EC_COORDINATE_BYTES);
		byte[] y = unsigned(key.getW().getAffineY(), EC_COORDINATE_BYTES);
		byte[] point = new byte[1 + 2 * EC_COORDINATE_BYTES];
		point[0] = 0x04;
		System.arraycopy(x, 0, point, 1, EC_COORDINATE_BYTES);
		System.arraycopy(y, 0, point, 1 + EC_COORDINATE_BYTES, EC_COORDINATE_BYTES);
		return b64(point);
	}

	private static byte[] unsigned(BigInteger value, int length) {
		byte[] raw = value.toByteArray();
		byte[] out = new byte[length];
		int copy = Math.min(raw.length, length);
		System.arraycopy(raw, raw.length - copy, out, length - copy, copy);
		return out;
	}

	private static String b64(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
