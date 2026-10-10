package com.github.angellariosacosta.bookingapp.application.port.in;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

public interface QueryAppointmentsUseCase {
	// Lanza AppointmentNotFoundException si la cita no existe o es de otro tenant.
	Appointment findById(Long id, Long userId);
	List<Appointment> findByCustomer(Long customerId, Long userId);
	List<Appointment> findByTimeSlot(Long userId, LocalDate date, LocalTime hour);
	// Citas activas del usuario que ocupan tiempo dentro del rango semiabierto [from, to).
	List<Appointment> findByDateRange(Long userId, LocalDateTime from, LocalDateTime to);
}
