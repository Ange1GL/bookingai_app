package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.angellariosacosta.bookingapp.application.service.PushPolicy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class PushConfig {

	@Bean
	PushPolicy pushPolicy(PushProperties properties) {
		if (!properties.isConfigured()) {
			log.warn("Notificaciones push desactivadas: faltan app.push.vapid-public-key, vapid-private-key o subject");
		}
		String publicKey = properties.isConfigured() ? properties.getVapidPublicKey() : "";
		return new PushPolicy(publicKey, properties.getLeadMinutes(), properties.getAllowedEndpointHosts());
	}
}
