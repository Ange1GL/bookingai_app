package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;

/**
 * Configuración de las notificaciones push. La construye la configuración ({@code app.push.*}) para que la
 * capa de aplicación no dependa de Spring Boot.
 *
 * @param vapidPublicKey              clave pública VAPID (vacía = push desactivado en este entorno)
 * @param leadMinutes                 cuántos minutos antes de la cita se avisa
 * @param allowedEndpointHostSuffixes servicios push permitidos: el servidor hace POST a cada endpoint
 *                                    suscrito, así que sin esta lista un usuario podría apuntarlo a una red interna
 */
public record PushPolicy(String vapidPublicKey, int leadMinutes, List<String> allowedEndpointHostSuffixes) {

	public PushPolicy {
		if (leadMinutes <= 0) {
			throw new IllegalArgumentException("Se requiere leadMinutes > 0");
		}
		allowedEndpointHostSuffixes = List.copyOf(allowedEndpointHostSuffixes);
	}

	public boolean configured() {
		return vapidPublicKey != null && !vapidPublicKey.isBlank();
	}
}
