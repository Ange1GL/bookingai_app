package com.github.angellariosacosta.bookingapp.application.result;

import java.time.OffsetDateTime;

/**
 * Dictados por voz del usuario en el día de negocio actual.
 *
 * @param dailyLimit       dictados que el usuario puede hacer por día
 * @param used             dictados hechos hoy
 * @param monthlyExhausted el tope mensual global se agotó: nadie puede dictar aunque le queden del día
 * @param resetsAt         inicio del siguiente día de negocio (el tope mensual se reinicia el día 1)
 */
public record VoiceQuota(int dailyLimit, int used, boolean monthlyExhausted, OffsetDateTime resetsAt) {

	private static final int PERCENT = 100;

	public int remaining() {
		return monthlyExhausted ? 0 : Math.max(0, dailyLimit - used);
	}

	public int percentUsed() {
		return monthlyExhausted ? PERCENT : Math.min(PERCENT, used * PERCENT / dailyLimit);
	}
}
