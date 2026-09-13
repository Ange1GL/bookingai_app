package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CancelAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CancelAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentAccessDeniedException;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
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
		Appointment appointment = appointmentRepository.findById(command.appointmentId())
				.orElseThrow(() -> new AppointmentNotFoundException(
						"Appointment not found with id: " + command.appointmentId()));

		if (!appointment.getUserId().equals(command.userId())) {
			throw new AppointmentAccessDeniedException(
					"User " + command.userId() + " is not allowed to cancel appointment " + command.appointmentId());
		}

		return appointmentRepository.updateStatus(command.appointmentId(), StatusAppointment.CANCELLED);
	}
}
