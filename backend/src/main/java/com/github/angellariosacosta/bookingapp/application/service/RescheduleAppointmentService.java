package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.RescheduleAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.RescheduleAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentAccessDeniedException;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RescheduleAppointmentService implements RescheduleAppointmentUseCase {

	private final AppointmentRepository appointmentRepository;

	@Override
	@Transactional
	public Appointment reschedule(RescheduleAppointmentCommand command) {
		Appointment appointment = appointmentRepository.findById(command.appointmentId())
				.orElseThrow(() -> new AppointmentNotFoundException(
						"Appointment not found with id: " + command.appointmentId()));

		if (!appointment.getUserId().equals(command.userId())) {
			throw new AppointmentAccessDeniedException(
					"User " + command.userId() + " is not allowed to reschedule appointment " + command.appointmentId());
		}

		Appointment.validateTimeRange(command.newStart(), command.newEnd());

		if (appointmentRepository.isOverlapping(command.newStart(), command.newEnd(), command.appointmentId())) {
			throw new AppointmentOverlapException(
				"The time slot %s – %s is already taken".formatted(command.newStart(), command.newEnd())
			);
		}
		return appointmentRepository.updateTimeSlot(command.appointmentId(), command.newStart(), command.newEnd());
	}
}
