package com.github.angellariosacosta.bookingapp.application.service;

/**
 * Límites de créditos de AI. Los construye la configuración ({@code assistant.credits.*}) para que
 * la capa de aplicación no dependa de Spring Boot.
 *
 * @param dailyTokenLimitPerUser tokens por usuario y día de negocio
 * @param globalDailyTokenLimit  tokens por día entre todos los usuarios: acota el gasto total
 */
public record AiCreditsPolicy(long dailyTokenLimitPerUser, long globalDailyTokenLimit) {

	public AiCreditsPolicy {
		if (dailyTokenLimitPerUser <= 0 || globalDailyTokenLimit < dailyTokenLimitPerUser) {
			throw new IllegalArgumentException("Se requiere 0 < dailyTokenLimitPerUser <= globalDailyTokenLimit");
		}
	}
}
