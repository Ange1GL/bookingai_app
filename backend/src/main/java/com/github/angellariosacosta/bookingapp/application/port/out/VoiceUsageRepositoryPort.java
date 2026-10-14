package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.LocalDate;

/**
 * Contadores de dictados por voz que acotan el gasto con el proveedor. Las operaciones
 * {@code tryConsume*} son atómicas: dos peticiones simultáneas nunca superan el límite.
 */
public interface VoiceUsageRepositoryPort {

	/** Suma un uso al día del usuario si no alcanzó {@code limit}; devuelve {@code false} si ya lo alcanzó. */
	boolean tryConsumeDaily(Long userId, LocalDate day, int limit);

	/** Suma un uso al mes (global, todos los usuarios) si no alcanzó {@code limit}. */
	boolean tryConsumeMonthly(LocalDate firstDayOfMonth, int limit);

	/** Devuelve un uso diario consumido sin éxito (nunca baja de cero). */
	void refundDaily(Long userId, LocalDate day);

	int dailyCount(Long userId, LocalDate day);
}
