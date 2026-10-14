package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Secretos del webhook de WhatsApp Cloud API ({@code whatsapp.webhook.*}). Ver docs/whatsapp-webhook.md. */
@Component
@ConfigurationProperties(prefix = "whatsapp.webhook")
@Validated
@Getter
@Setter
public class WhatsAppProperties {

	/** Cadena que se define en el panel de Meta; Meta la envía en el GET de verificación. */
	@NotBlank
	private String verifyToken;

	/** App Secret de la app de Meta; firma (HMAC-SHA256) el cuerpo de cada POST. */
	@NotBlank
	private String appSecret;
}
