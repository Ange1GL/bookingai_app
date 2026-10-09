package com.github.angellariosacosta.bookingapp.domain.model;

/**
 * Regla de auto-bloqueo: un cliente entra a la lista negra al acumular {@code noShowThreshold}
 * inasistencias activas. Un umbral menor o igual a 0 desactiva el auto-bloqueo.
 */
public record BlacklistPolicy(int noShowThreshold) {

	public boolean isReached(long activeNoShows) {
		return noShowThreshold > 0 && activeNoShows >= noShowThreshold;
	}
}
