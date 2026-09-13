package com.github.angellariosacosta.bookingapp.domain.model;


import java.time.LocalDateTime;

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


	public static void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
		if (!startTime.isBefore(endTime)) {
			throw new InvalidAppointmentTimeRangeException("startTime must be before endTime");
		}
	}

}
