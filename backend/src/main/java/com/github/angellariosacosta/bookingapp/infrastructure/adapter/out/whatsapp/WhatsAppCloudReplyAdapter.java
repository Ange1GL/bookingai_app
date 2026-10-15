package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerReplyPort;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;
import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppCloudProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Envía la respuesta al cliente por la Cloud API de WhatsApp. Un fallo de envío se registra pero no se propaga:
 * no hay a quién reintentar (Meta ya recibió su 200 del webhook). Nunca se loguea el token ni el texto enviado.
 */
@Slf4j
public class WhatsAppCloudReplyAdapter implements CustomerReplyPort {

	static final int MAX_TEXT_LENGTH = 4096;
	private static final Pattern PHONE_NUMBER_ID = Pattern.compile("\\d{1,32}");
	private static final int MAX_LOGGED_ERROR_BODY = 300;

	private final RestClient restClient;
	private final WhatsAppCloudProperties properties;

	public WhatsAppCloudReplyAdapter(RestClient restClient, WhatsAppCloudProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public void send(String phoneNumberId, String to, String text) {
		if (!PHONE_NUMBER_ID.matcher(phoneNumberId).matches()) {
			log.error("phone_number_id inválido en el evento de WhatsApp, no se envía la respuesta");
			return;
		}
		try {
			restClient.post()
					.uri("/{version}/{phoneNumberId}/messages", properties.getApiVersion(), phoneNumberId)
					.header("Authorization", "Bearer " + properties.getSystemUserToken())
					.contentType(MediaType.APPLICATION_JSON)
					.body(TextMessageRequest.of(to, truncate(text)))
					.retrieve()
					.toBodilessEntity();
			log.info("WhatsApp respuesta enviada to={}", PhoneMask.mask(to));
		} catch (RestClientResponseException ex) {
			log.error("Meta rechazó el envío a {}: status={} body={}", PhoneMask.mask(to), ex.getStatusCode().value(),
					abbreviate(ex.getResponseBodyAsString()));
		} catch (RestClientException ex) {
			log.error("No se pudo enviar la respuesta a {}: {}", PhoneMask.mask(to), ex.getMessage());
		}
	}

	private static String truncate(String text) {
		return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
	}

	private static String abbreviate(String body) {
		return body.length() <= MAX_LOGGED_ERROR_BODY ? body : body.substring(0, MAX_LOGGED_ERROR_BODY) + "…";
	}

	record TextMessageRequest(String messaging_product, String recipient_type, String to, String type, Text text) {

		private static final String PRODUCT = "whatsapp";
		private static final String INDIVIDUAL = "individual";
		private static final String TEXT_TYPE = "text";

		static TextMessageRequest of(String to, String body) {
			return new TextMessageRequest(PRODUCT, INDIVIDUAL, to, TEXT_TYPE, new Text(false, body));
		}

		record Text(boolean preview_url, String body) {
		}
	}
}
