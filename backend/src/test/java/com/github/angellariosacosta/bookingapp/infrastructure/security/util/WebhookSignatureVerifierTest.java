package com.github.angellariosacosta.bookingapp.infrastructure.security.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppProperties;

class WebhookSignatureVerifierTest {

	private static final String APP_SECRET = "test-app-secret";
	private static final String PREFIX = "sha256=";
	private static final byte[] BODY = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

	private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(properties());

	@Test
	void acceptsValidSignature() throws Exception {
		assertTrue(verifier.isValid(BODY, sign(BODY, APP_SECRET)));
	}

	@Test
	void rejectsSignatureMadeWithAnotherSecret() throws Exception {
		assertFalse(verifier.isValid(BODY, sign(BODY, "otro-secreto")));
	}

	@Test
	void rejectsTamperedBody() throws Exception {
		byte[] tampered = BODY.clone();
		tampered[2] ^= 1;
		assertFalse(verifier.isValid(tampered, sign(BODY, APP_SECRET)));
	}

	@Test
	void rejectsNullOrMalformedHeader() throws Exception {
		assertFalse(verifier.isValid(BODY, null));
		assertFalse(verifier.isValid(BODY, ""));
		assertFalse(verifier.isValid(BODY, sign(BODY, APP_SECRET).substring(PREFIX.length())));
	}

	private static WhatsAppProperties properties() {
		WhatsAppProperties properties = new WhatsAppProperties();
		properties.setAppSecret(APP_SECRET);
		return properties;
	}

	private static String sign(byte[] body, String secret) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return PREFIX + HexFormat.of().formatHex(mac.doFinal(body));
	}
}
