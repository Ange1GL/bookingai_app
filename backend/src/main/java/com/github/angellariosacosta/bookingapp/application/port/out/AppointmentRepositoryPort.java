package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;

public interface AppointmentRepositoryPort {
	Appointment save(Appointment save);
	Optional<Appointment> findById(Long id, Long userId);
	boolean isOverlapping(LocalDateTime startTime, LocalDateTime endTime);
	boolean isOverlapping(LocalDateTime startTime, LocalDateTime endTime, Long excludeAppointmentId);
	List<Appointment> findByCustomerId(Long customerId, Long userId);
	// Sin scope por userId a propósito: se usa para chequear disponibilidad de horario
	// entre TODOS los barberos, no solo el usuario actual.
	List<Appointment> findByTimeSlot(LocalDateTime from, LocalDateTime to);
	Appointment updateStatus(Long id, Long userId, StatusAppointment newStatus);
	Appointment updateTimeSlot(Long id, Long userId, LocalDateTime newStart, LocalDateTime newEnd);
}

