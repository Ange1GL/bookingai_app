package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.Instant;
import java.util.List;

import com.github.angellariosacosta.bookingapp.domain.model.PasswordRecoveryRequest;

public interface PasswordRecoveryRequestRepositoryPort {

	/** Registra la solicitud pendiente del usuario; si ya tenía una, no hace nada (a lo mucho una por usuario). */
	void registerPending(Long userId, Instant now);

	/** Marca como atendida la solicitud pendiente del usuario, si la hay. */
	void resolvePending(Long userId, Instant now);

	/** Solicitudes sin atender, de la más antigua a la más reciente. */
	List<PasswordRecoveryRequest> findPending();
}
