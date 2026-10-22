package com.github.angellariosacosta.bookingapp.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.github.angellariosacosta.bookingapp.application.service.PushPolicy;

class PushConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(PushConfig.class, EnableProps.class)
			.withPropertyValues(
					"app.push.lead-minutes=5",
					"app.push.job-interval-seconds=30",
					"app.push.allowed-endpoint-hosts=fcm.googleapis.com,push.apple.com");

	@EnableConfigurationProperties(PushProperties.class)
	static class EnableProps {
	}

	@Test
	@DisplayName("con claves y contacto el push queda configurado")
	void configuredWithKeys() {
		runner.withPropertyValues("app.push.vapid-public-key=pub", "app.push.vapid-private-key=priv",
				"app.push.subject=mailto:a@b.com").run(context -> {
			PushPolicy policy = context.getBean(PushPolicy.class);
			assertTrue(policy.configured());
			assertEquals("pub", policy.vapidPublicKey());
			assertEquals(5, policy.leadMinutes());
			assertEquals(List.of("fcm.googleapis.com", "push.apple.com"), policy.allowedEndpointHostSuffixes());
		});
	}

	@Test
	@DisplayName("sin claves el contexto arranca con el push desactivado")
	void disabledWithoutKeys() {
		runner.run(context -> {
			assertFalse(context.getBean(PushPolicy.class).configured());
		});
	}

	@Test
	@DisplayName("con la clave pública pero sin la privada no se activa a medias")
	void partialKeysStayDisabled() {
		runner.withPropertyValues("app.push.vapid-public-key=pub", "app.push.subject=mailto:a@b.com").run(context -> {
			assertFalse(context.getBean(PushPolicy.class).configured());
		});
	}
}
