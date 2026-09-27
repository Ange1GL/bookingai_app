package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CancelAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CancelAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CancelAppointmentService implements CancelAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepository;

	@Override
	@Transactional
	public Appointment cancel(CancelAppointmentCommand command) {
		appointmentRepository.findById(command.appointmentId(), command.userId())
				.orElseThrow(() -> new AppointmentNotFoundException(
						"Appointment not found with id: " + command.appointmentId()));

		return appointmentRepository.updateStatus(command.appointmentId(), command.userId(), StatusAppointment.CANCELLED);
	}
}
