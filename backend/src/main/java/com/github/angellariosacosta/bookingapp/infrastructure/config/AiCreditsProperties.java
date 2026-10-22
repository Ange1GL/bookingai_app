package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Créditos de AI del asistente de texto ({@code assistant.credits.*}). Ver docs/ai-credits.md. */
@Component
@ConfigurationProperties(prefix = "assistant.credits")
@Validated
@Getter
@Setter
public class AiCreditsProperties {

	@Positive
	private long dailyTokenLimitPerUser;

	@Positive
	private long globalDailyTokenLimit;
}
