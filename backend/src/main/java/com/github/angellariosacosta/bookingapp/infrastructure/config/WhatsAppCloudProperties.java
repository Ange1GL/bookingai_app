package com.github.angellariosacosta.bookingapp.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Envío de mensajes por la Cloud API ({@code whatsapp.cloud.*}). Sin token (variable {@code SYSTEM_USER_EMPLOYEE_META}
 * vacía) la app arranca en modo demo: la respuesta solo se registra en el log. Ver docs/whatsapp-webhook.md.
 */
@Component
@ConfigurationProperties(prefix = "whatsapp.cloud")
@Getter
@Setter
public class WhatsAppCloudProperties {

	private String baseUrl = "https://graph.facebook.com";

	private String apiVersion = "v26.0";

	/** Token permanente del usuario del sistema de Meta; secreto, nunca se loguea. */
	private String systemUserToken = "";

	private Duration connectTimeout = Duration.ofSeconds(5);

	private Duration readTimeout = Duration.ofSeconds(10);

	/** Número emisor (phone_number_id) desde el que se envían las contraseñas temporales. */
	private String credentialsPhoneNumberId = "";

	/** Plantilla aprobada por Meta con dos variables de cuerpo: {{1}} usuario y {{2}} contraseña. */
	private String credentialsTemplateName = "";

	private String credentialsTemplateLanguage = "es";

	public boolean isSenderEnabled() {
		return systemUserToken != null && !systemUserToken.isBlank();
	}

	public boolean isCredentialsSenderEnabled() {
		return isSenderEnabled()
				&& credentialsPhoneNumberId != null && !credentialsPhoneNumberId.isBlank()
				&& credentialsTemplateName != null && !credentialsTemplateName.isBlank();
	}
}
