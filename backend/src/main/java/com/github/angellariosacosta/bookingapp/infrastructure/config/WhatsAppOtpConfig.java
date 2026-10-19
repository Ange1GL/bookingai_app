package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeNotifierPort;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp.LoggingOneTimeCodeNotifierAdapter;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp.WhatsAppOneTimeCodeNotifierAdapter;

import lombok.extern.slf4j.Slf4j;

/** Elige cómo se entregan los códigos: plantilla de WhatsApp si todo está configurado, o nada (solo log). */
@Slf4j
@Configuration
public class WhatsAppOtpConfig {

	@Bean
	public OneTimeCodeNotifierPort oneTimeCodeNotifierPort(
			WhatsAppCloudProperties properties, RestClient whatsappRestClient) {
		if (!properties.isOtpSenderEnabled()) {
			log.warn("WhatsApp de códigos sin configurar (token, número emisor o plantilla): los códigos de un solo "
					+ "uso no se enviarán");
			return new LoggingOneTimeCodeNotifierAdapter();
		}
		log.info("Envío de códigos de un solo uso por plantilla de WhatsApp habilitado");
		return new WhatsAppOneTimeCodeNotifierAdapter(whatsappRestClient, properties);
	}
}
