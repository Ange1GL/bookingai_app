package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.LocalDateTime;

import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

public interface AppointmentRepository {
	Appointment save(Appointment save);
	
	boolean isOverlapping(LocalDateTime startTime, LocalDateTime endTime);
}

