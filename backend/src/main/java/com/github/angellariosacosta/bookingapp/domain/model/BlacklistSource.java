package com.github.angellariosacosta.bookingapp.domain.model;

public enum BlacklistSource {
	// El barbero lo agrego a la lista negra a mano.
	MANUAL,
	// Se agrego solo al alcanzar el umbral de inasistencias (BlacklistPolicy).
	AUTO_NO_SHOW
}
