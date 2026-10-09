package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.angellariosacosta.bookingapp.domain.model.BlacklistPolicy;

import lombok.RequiredArgsConstructor;

/** Expone la regla de auto-bloqueo al dominio sin que {@code application/} dependa de la config. */
@Configuration
@RequiredArgsConstructor
public class BlacklistConfig {

	private final BlacklistProperties properties;

	@Bean
	public BlacklistPolicy blacklistPolicy() {
		return new BlacklistPolicy(properties.getNoShowThreshold());
	}
}
