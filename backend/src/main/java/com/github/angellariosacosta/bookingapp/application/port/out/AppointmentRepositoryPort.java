package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;

public interface AppointmentRepositoryPort {
	Appointment save(Appointment save);
	Optional<Appointment> findById(Long id, Long userId);
	boolean isOverlapping(Long userId, LocalDateTime startTime, LocalDateTime endTime );
	boolean isOverlapping(Long userId, LocalDateTime startTime, LocalDateTime endTime,  Long excludeAppointmentId);
	List<Appointment> findByCustomerId(Long customerId, Long userId);
	// Scopeado por userId: solo devuelve las citas activas del dueño actual que tocan [from, to].
	List<Appointment> findByTimeSlot(Long userId, LocalDateTime from, LocalDateTime to);
	Appointment updateStatus(Long id, Long userId, StatusAppointment newStatus);
	// Cancela en una sola sentencia las citas RESERVED del cliente que empiezan en o despues de "from";
	// devuelve cuantas se cancelaron.
	int cancelReservedFrom(Long customerId, Long userId, LocalDateTime from);
	Appointment updateTimeSlot(Long id, Long userId, LocalDateTime newStart, LocalDateTime newEnd);
}

