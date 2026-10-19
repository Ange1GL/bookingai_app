package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.github.angellariosacosta.bookingapp.application.port.out.UserCredentialNotifierPort;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;
import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppCloudProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Envía la contraseña temporal con una plantilla aprobada de WhatsApp (los mensajes iniciados por el negocio fuera de la
 * ventana de 24 h solo pueden ser plantillas). Parámetros del cuerpo: {{1}} = usuario, {{2}} = contraseña. Nunca se
 * loguea la contraseña ni el cuerpo del request.
 */
@Slf4j
public class WhatsAppCredentialNotifierAdapter implements UserCredentialNotifierPort {

	private static final Pattern PHONE_NUMBER_ID = Pattern.compile("\\d{1,32}");
	private static final int MAX_LOGGED_ERROR_BODY = 300;

	private final RestClient restClient;
	private final WhatsAppCloudProperties properties;

	public WhatsAppCredentialNotifierAdapter(RestClient restClient, WhatsAppCloudProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public boolean sendTemporaryPassword(String phone, String username, String temporaryPassword) {
		String phoneNumberId = properties.getCredentialsPhoneNumberId();
		if (!PHONE_NUMBER_ID.matcher(phoneNumberId).matches()) {
			log.error("whatsapp.cloud.credentials-phone-number-id inválido, no se envía la contraseña temporal");
			return false;
		}
		try {
			restClient.post()
					.uri("/{version}/{phoneNumberId}/messages", properties.getApiVersion(), phoneNumberId)
					.header("Authorization", "Bearer " + properties.getSystemUserToken())
					.contentType(MediaType.APPLICATION_JSON)
					.body(TemplateMessageRequest.of(phone, properties.getCredentialsTemplateName(),
							properties.getCredentialsTemplateLanguage(), username, temporaryPassword))
					.retrieve()
					.toBodilessEntity();
			log.info("Contraseña temporal enviada por WhatsApp to={}", PhoneMask.mask(phone));
			return true;
		} catch (RestClientResponseException ex) {
			log.error("Meta rechazó el envío de credenciales a {}: status={} body={}", PhoneMask.mask(phone),
					ex.getStatusCode().value(), abbreviate(ex.getResponseBodyAsString()));
		} catch (RestClientException ex) {
			log.error("No se pudo enviar las credenciales a {}: {}", PhoneMask.mask(phone), ex.getClass().getSimpleName());
		}
		return false;
	}

	private static String abbreviate(String body) {
		return body.length() <= MAX_LOGGED_ERROR_BODY ? body : body.substring(0, MAX_LOGGED_ERROR_BODY) + "…";
	}

	record TemplateMessageRequest(String messaging_product, String to, String type, Template template) {

		private static final String PRODUCT = "whatsapp";
		private static final String TEMPLATE_TYPE = "template";
		private static final String TEXT = "text";
		private static final String BODY = "body";

		static TemplateMessageRequest of(String to, String templateName, String languageCode, String... bodyParameters) {
			List<Parameter> parameters = java.util.Arrays.stream(bodyParameters)
					.map(value -> new Parameter(TEXT, value))
					.toList();
			Template template = new Template(templateName, new Language(languageCode),
					List.of(new Component(BODY, parameters)));
			return new TemplateMessageRequest(PRODUCT, to, TEMPLATE_TYPE, template);
		}

		record Template(String name, Language language, List<Component> components) {
		}

		record Language(String code) {
		}

		record Component(String type, List<Parameter> parameters) {
		}

		record Parameter(String type, String text) {
		}
	}
}
