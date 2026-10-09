package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.Instant;
import java.util.List;

import com.github.angellariosacosta.bookingapp.domain.model.NoShow;

public interface NoShowRepositoryPort {
	NoShow save(NoShow noShow);
	boolean existsByAppointmentId(Long appointmentId, Long userId);
	// Solo cuenta las inasistencias vigentes (no "perdonadas" al salir de la lista negra).
	long countActiveByCustomerId(Long customerId, Long userId);
	List<NoShow> findByCustomerId(Long customerId, Long userId);
	// Marca como perdonadas las inasistencias vigentes del cliente; devuelve cuántas.
	int clearByCustomerId(Long customerId, Long userId, Instant clearedAt);
}
