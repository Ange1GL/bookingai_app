package com.github.angellariosacosta.bookingapp.application.service;

/**
 * Límites del dictado por voz. Los construye la configuración ({@code assistant.voice.*}) para que
 * la capa de aplicación no dependa de Spring Boot.
 *
 * @param minAudioBytes      por debajo se considera un audio vacío o un toque accidental
 * @param maxAudioBytes      tope de tamaño (30 s de opus ocupan ~110 KB; Safari/AAC pesa más)
 * @param dailyLimitPerUser  dictados por usuario y día de negocio
 * @param monthlyLimit       dictados por mes entre todos los usuarios: acota el gasto total
 */
public record VoiceTranscriptionPolicy(int minAudioBytes, int maxAudioBytes, int dailyLimitPerUser, int monthlyLimit) {

	public VoiceTranscriptionPolicy {
		if (minAudioBytes <= 0 || maxAudioBytes <= minAudioBytes) {
			throw new IllegalArgumentException("Se requiere 0 < minAudioBytes < maxAudioBytes");
		}
		if (dailyLimitPerUser <= 0 || monthlyLimit < dailyLimitPerUser) {
			throw new IllegalArgumentException("Se requiere 0 < dailyLimitPerUser <= monthlyLimit");
		}
	}
}
