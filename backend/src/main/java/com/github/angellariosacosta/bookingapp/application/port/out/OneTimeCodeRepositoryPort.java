package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.Instant;
import java.util.Optional;

import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;

public interface OneTimeCodeRepositoryPort {

	OneTimeCode save(OneTimeCode code);

	/** El código aún sin consumir del usuario (puede estar vencido), bloqueado para actualizarlo sin carreras. */
	Optional<OneTimeCode> findPendingByUserIdForUpdate(Long userId);

	/** Invalida (consume) el código pendiente del usuario, si lo hay. */
	void consumePending(Long userId, Instant now);
}
