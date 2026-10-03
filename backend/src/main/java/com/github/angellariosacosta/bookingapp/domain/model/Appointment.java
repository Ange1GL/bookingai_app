package com.github.angellariosacosta.bookingapp.domain.model;


import java.time.Duration;
import java.time.LocalDateTime;

import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentInPastException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidAppointmentTimeRangeException;

import lombok.Getter;
import lombok.Setter;


@Getter
public class Appointment {

	@Setter
	private Long id;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private
	Customer customer;
	private StatusAppointment status;
	private Long userId;


	public static Appointment createNew(
			LocalDateTime startTime,
			LocalDateTime endTime,
			Customer customer,
			Long userId
			) {
		return new Appointment(startTime, endTime, customer, StatusAppointment.PENDING, userId);
	}


	public Appointment(
			LocalDateTime startTime,
			LocalDateTime endTime,
			Customer customer,
			StatusAppointment status,
			Long userId
			) {
		validateTimeRange(startTime, endTime);
		this.startTime = startTime;
		this.endTime = endTime;
		this.customer = customer;
		this.status = status;
		this.userId = userId;
	}


	// Tolerancia para no rechazar una cita "de ahora mismo" que llega con unos segundos de retraso.
	private static final Duration PAST_TOLERANCE = Duration.ofMinutes(5);

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
