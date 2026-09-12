package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CancelAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CancelAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CancelAppointmentService implements CancelAppointmentUseCase {

	private final AppointmentRepository appointmentRepository;

	@Override
	@Transactional
	public Appointment cancel(CancelAppointmentCommand command) {
		return appointmentRepository.updateStatus(command.appointmentId(), StatusAppointment.CANCELLED);
	}
}
