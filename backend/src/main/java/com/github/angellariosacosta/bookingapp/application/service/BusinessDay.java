package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Día de negocio (zona de {@link Clock}): los contadores diarios de uso se reinician en su medianoche. */
final class BusinessDay {

	private BusinessDay() {
	}

	static LocalDate today(Clock clock) {
		return LocalDate.now(clock);
	}

	/** Inicio del siguiente día de negocio: cuando vuelven a cero las cuotas diarias. */
	static OffsetDateTime nextStart(Clock clock) {
		return today(clock).plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
	}
}
