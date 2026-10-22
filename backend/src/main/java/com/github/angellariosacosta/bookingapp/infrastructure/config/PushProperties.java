package com.github.angellariosacosta.bookingapp.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Recordatorios push ({@code app.push.*}). Sin claves VAPID el push queda desactivado y la app arranca igual.
 * Ver docs/push-reminders.md.
 */
@Component
@ConfigurationProperties(prefix = "app.push")
@Validated
@Getter
@Setter
public class PushProperties {

	/** Claves en base64url (script scripts/generate-vapid-keys.mjs). La privada nunca va en el repo. */
	private String vapidPublicKey = "";
	private String vapidPrivateKey = "";

	/** Contacto que el servicio push puede usar si algo falla (mailto: o https:). */
	private String subject = "";

	@Positive
	private int leadMinutes;

	@Positive
	private int jobIntervalSeconds;

	@NotEmpty
	private List<String> allowedEndpointHosts;

	public boolean isConfigured() {
		return !vapidPublicKey.isBlank() && !vapidPrivateKey.isBlank() && !subject.isBlank();
	}
}
