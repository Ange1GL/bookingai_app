package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.github.angellariosacosta.bookingapp.application.port.out.UserCredentialNotifierPort;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp.LoggingUserCredentialNotifierAdapter;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp.WhatsAppCredentialNotifierAdapter;

import lombok.extern.slf4j.Slf4j;

/** Elige cómo se entregan las contraseñas temporales: plantilla de WhatsApp si todo está configurado, o nada (solo log). */
@Slf4j
@Configuration
public class WhatsAppCredentialsConfig {

	@Bean
	public UserCredentialNotifierPort userCredentialNotifierPort(
			WhatsAppCloudProperties properties, RestClient whatsappRestClient) {
		if (!properties.isCredentialsSenderEnabled()) {
			log.warn("WhatsApp de credenciales sin configurar (token, número emisor o plantilla): las contraseñas "
					+ "temporales no se enviarán");
			return new LoggingUserCredentialNotifierAdapter();
		}
		log.info("Envío de contraseñas temporales por plantilla de WhatsApp habilitado");
		return new WhatsAppCredentialNotifierAdapter(whatsappRestClient, properties);
	}
}
