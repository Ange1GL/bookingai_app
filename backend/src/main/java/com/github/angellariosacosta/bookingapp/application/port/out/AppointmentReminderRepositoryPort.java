package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

import com.github.angellariosacosta.bookingapp.application.result.AppointmentReminder;

public interface AppointmentReminderRepositoryPort {

	/**
	 * Reclama de forma atómica las citas reservadas de usuarios con dispositivos suscritos que empiezan en
	 * ({@code from}, {@code to}] y aún no se avisaron: las marca como avisadas y las devuelve en la misma
	 * sentencia. Dos instancias de la app nunca reclaman la misma cita.
	 */
	List<AppointmentReminder> claimDue(LocalDateTime from, LocalDateTime to);
}
