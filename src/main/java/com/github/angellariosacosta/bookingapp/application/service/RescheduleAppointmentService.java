package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.RescheduleAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.RescheduleAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
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
		if (appointmentRepository.isOverlapping(command.newStart(), command.newEnd())) {
			throw new AppointmentOverlapException(
				"The time slot %s – %s is already taken".formatted(command.newStart(), command.newEnd())
			);
		}
		return appointmentRepository.updateTimeSlot(command.appointmentId(), command.newStart(), command.newEnd());
	}
}
