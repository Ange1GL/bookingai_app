package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;

public interface AppointmentRepository {
	Appointment save(Appointment save);
	boolean isOverlapping(LocalDateTime startTime, LocalDateTime endTime);
	List<Appointment> findByCustomerId(Long customerId);
	List<Appointment> findByTimeSlot(LocalDateTime from, LocalDateTime to);
	Appointment updateStatus(Long id, StatusAppointment newStatus);
	Appointment updateTimeSlot(Long id, LocalDateTime newStart, LocalDateTime newEnd);
}

