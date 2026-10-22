package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.angellariosacosta.bookingapp.application.service.AiCreditsPolicy;

@Configuration
public class AiCreditsConfig {

	@Bean
	AiCreditsPolicy aiCreditsPolicy(AiCreditsProperties properties) {
		return new AiCreditsPolicy(properties.getDailyTokenLimitPerUser(), properties.getGlobalDailyTokenLimit());
	}
}
