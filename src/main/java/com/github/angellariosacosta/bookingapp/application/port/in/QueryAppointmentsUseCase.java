package com.github.angellariosacosta.bookingapp.application.port.in;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

public interface QueryAppointmentsUseCase {
	List<Appointment> findByCustomer(Long customerId);
	List<Appointment> findByTimeSlot(LocalDate date, LocalTime hour);
}
