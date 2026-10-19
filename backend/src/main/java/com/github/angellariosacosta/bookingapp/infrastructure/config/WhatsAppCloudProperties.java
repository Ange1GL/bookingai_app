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

	/** Número emisor (phone_number_id) desde el que se envían los códigos de un solo uso. */
	private String otpPhoneNumberId = "";

	/** Plantilla de autenticación aprobada por Meta: el código va en el cuerpo y en el botón "Copiar código". */
	private String otpTemplateName = "";

	private String otpTemplateLanguage = "es";

	public boolean isSenderEnabled() {
		return systemUserToken != null && !systemUserToken.isBlank();
	}

	public boolean isOtpSenderEnabled() {
		return isSenderEnabled()
				&& otpPhoneNumberId != null && !otpPhoneNumberId.isBlank()
				&& otpTemplateName != null && !otpTemplateName.isBlank();
	}
}
