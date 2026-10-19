package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeNotifierPort;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;
import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppCloudProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Envía el código con una plantilla de <b>autenticación</b> de WhatsApp (el negocio inicia la conversación, así que solo
 * pueden ser plantillas). Ese tipo lleva el código dos veces: como parámetro del cuerpo y como parámetro del botón
 * "Copiar código". Nunca se loguea el código ni el cuerpo del request.
 */
@Slf4j
public class WhatsAppOneTimeCodeNotifierAdapter implements OneTimeCodeNotifierPort {

	private static final Pattern PHONE_NUMBER_ID = Pattern.compile("\\d{1,32}");
	private static final int MAX_LOGGED_ERROR_BODY = 300;

	private final RestClient restClient;
	private final WhatsAppCloudProperties properties;

	public WhatsAppOneTimeCodeNotifierAdapter(RestClient restClient, WhatsAppCloudProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public boolean sendCode(String phone, String code) {
		String phoneNumberId = properties.getOtpPhoneNumberId();
		if (!PHONE_NUMBER_ID.matcher(phoneNumberId).matches()) {
			log.error("whatsapp.cloud.otp-phone-number-id inválido, no se envía el código");
			return false;
		}
		try {
			restClient.post()
					.uri("/{version}/{phoneNumberId}/messages", properties.getApiVersion(), phoneNumberId)
					.header("Authorization", "Bearer " + properties.getSystemUserToken())
					.contentType(MediaType.APPLICATION_JSON)
					.body(TemplateMessageRequest.of(phone, properties.getOtpTemplateName(),
							properties.getOtpTemplateLanguage(), code))
					.retrieve()
					.toBodilessEntity();
			log.info("Código enviado por WhatsApp to={}", PhoneMask.mask(phone));
			return true;
		} catch (RestClientResponseException ex) {
			log.error("Meta rechazó el envío del código a {}: status={} body={}", PhoneMask.mask(phone),
					ex.getStatusCode().value(), abbreviate(ex.getResponseBodyAsString()));
		} catch (RestClientException ex) {
			log.error("No se pudo enviar el código a {}: {}", PhoneMask.mask(phone), ex.getClass().getSimpleName());
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
		private static final String BUTTON = "button";
		private static final String URL_SUB_TYPE = "url";
		private static final String FIRST_BUTTON_INDEX = "0";

		static TemplateMessageRequest of(String to, String templateName, String languageCode, String code) {
			List<Parameter> parameters = List.of(new Parameter(TEXT, code));
			List<Component> components = List.of(
					new Component(BODY, null, null, parameters),
					new Component(BUTTON, URL_SUB_TYPE, FIRST_BUTTON_INDEX, parameters));
			return new TemplateMessageRequest(PRODUCT, to, TEMPLATE_TYPE,
					new Template(templateName, new Language(languageCode), components));
		}

		record Template(String name, Language language, List<Component> components) {
		}

		record Language(String code) {
		}

		// sub_type e index solo aplican al botón; en el cuerpo van null y Jackson los omite si se configura NON_NULL.
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
		record Component(String type, String sub_type, String index, List<Parameter> parameters) {
		}

		record Parameter(String type, String text) {
		}
	}
}
