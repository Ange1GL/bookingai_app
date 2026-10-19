package com.github.angellariosacosta.bookingapp.infrastructure.config;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerReplyPort;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp.LoggingCustomerReplyAdapter;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp.WhatsAppCloudReplyAdapter;

import lombok.extern.slf4j.Slf4j;

/** Elige cómo se entregan las respuestas: Cloud API real si hay token, o solo log (demo) si no. */
@Slf4j
@Configuration
public class WhatsAppReplyConfig {

	@Bean
	public CustomerReplyPort customerReplyPort(WhatsAppCloudProperties properties, RestClient whatsappRestClient) {
		if (!properties.isSenderEnabled()) {
			log.warn("SYSTEM_USER_EMPLOYEE_META vacío: las respuestas de WhatsApp solo se registran en el log (modo demo)");
			return new LoggingCustomerReplyAdapter();
		}
		log.info("Envío de respuestas por la Cloud API de WhatsApp habilitado ({})", properties.getApiVersion());
		return new WhatsAppCloudReplyAdapter(whatsappRestClient, properties);
	}

	/** Cliente HTTP de la Cloud API, compartido por el envío de respuestas y el de contraseñas temporales. */
	@Bean
	public RestClient whatsappRestClient(WhatsAppCloudProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.getReadTimeout());
		return RestClient.builder()
				.baseUrl(properties.getBaseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
