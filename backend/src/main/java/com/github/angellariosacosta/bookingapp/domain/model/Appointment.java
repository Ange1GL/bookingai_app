package com.github.angellariosacosta.bookingapp.domain.model;


import java.time.Duration;
import java.time.LocalDateTime;

import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentInPastException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidAppointmentTimeRangeException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidStatusTransitionException;
import com.github.angellariosacosta.bookingapp.domain.exception.NoShowNotAllowedException;

import lombok.Getter;
import lombok.Setter;


@Getter
public class Appointment {

	// Tolerancia para no rechazar una cita "de ahora mismo" que llega con unos segundos de retraso.
	private static final Duration PAST_TOLERANCE = Duration.ofMinutes(5);

	@Setter
	private Long id;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private
	Customer customer;
	private StatusAppointment status;
	private Long userId;
	private PriceCatalog priceCatalog;


	public static Appointment createNew(
			LocalDateTime startTime,
			LocalDateTime endTime,
			Customer customer,
			Long userId,
			PriceCatalog priceCatalog,
			LocalDateTime now
			) {
		validateTimeRange(startTime, endTime);
		validateNotInPast(startTime, now);
		return new Appointment(null, startTime, endTime, customer, StatusAppointment.RESERVED, userId, priceCatalog);
	}

	// Reconstruye una cita ya guardada (uso del adaptador de persistencia). No aplica reglas de
	// creacion: los datos se validaron cuando la cita se creo y aqui solo se rehidratan.
	public static Appointment reconstitute(
			Long id,
			LocalDateTime startTime,
			LocalDateTime endTime,
			Customer customer,
			StatusAppointment status,
			Long userId,
			PriceCatalog priceCatalog
			) {
		return new Appointment(id, startTime, endTime, customer, status, userId, priceCatalog);
	}


	private Appointment(
			Long id,
			LocalDateTime startTime,
			LocalDateTime endTime,
			Customer customer,
			StatusAppointment status,
			Long userId,
			PriceCatalog priceCatalog
			) {
		if (priceCatalog == null) {
			throw new InvalidFieldException("priceCatalog", "must not be null");
		}
		this.id = id;
		this.startTime = startTime;
		this.endTime = endTime;
		this.customer = customer;
		this.status = status;
		this.userId = userId;
		this.priceCatalog = priceCatalog;
	}


	// Solo una cita RESERVED puede cancelarse o reagendarse; una CANCELLED ya no admite cambios.
	public void ensureReserved() {
		if (status != StatusAppointment.RESERVED) {
			throw new InvalidStatusTransitionException(
					"Appointment %d is %s; only RESERVED appointments can be modified".formatted(id, status));
		}
	}

	// Un no-show solo aplica a una cita vigente (RESERVED) cuyo horario ya comenzo: no se puede
	// marcar inasistencia de una cita cancelada ni de una que todavia no llega.
	public void ensureNoShowRegistrable(LocalDateTime now) {
		if (status != StatusAppointment.RESERVED) {
			throw new NoShowNotAllowedException(
					"Appointment %d is %s; only RESERVED appointments can be marked as no-show".formatted(id, status));
		}
		if (startTime.isAfter(now)) {
			throw new NoShowNotAllowedException(
					"Appointment %d has not started yet; it cannot be marked as no-show".formatted(id));
		}
	}

	// "now" debe venir de la hora del negocio (Clock configurado), nunca de la zona de la JVM.
	public static void validateNotInPast(LocalDateTime startTime, LocalDateTime now) {
		if (startTime.isBefore(now.minus(PAST_TOLERANCE))) {
			throw new AppointmentInPastException("startTime cannot be in the past");
		}
	}

	public static void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
		if (!startTime.isBefore(endTime)) {
			throw new InvalidAppointmentTimeRangeException("startTime must be before endTime");
		}
	}

}
