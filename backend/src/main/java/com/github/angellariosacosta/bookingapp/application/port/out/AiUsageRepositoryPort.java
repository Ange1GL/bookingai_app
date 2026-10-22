package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.LocalDate;

public interface AiUsageRepositoryPort {

	long usedByUser(Long userId, LocalDate day);

	long usedGlobally(LocalDate day);

	/** Suma los tokens al contador del usuario y al global del día; ambos o ninguno (lo garantiza la transacción del llamador). */
	void addUsage(Long userId, LocalDate day, long tokens);
}
