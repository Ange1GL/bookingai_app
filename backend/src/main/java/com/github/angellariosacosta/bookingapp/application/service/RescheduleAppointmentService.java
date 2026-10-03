package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.RescheduleAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.RescheduleAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RescheduleAppointmentService implements RescheduleAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepository;
	private final Clock clock;

	@Override
	@Transactional
	public Appointment reschedule(RescheduleAppointmentCommand command) {
		appointmentRepository.findById(command.appointmentId(), command.userId())
				.orElseThrow(() -> new AppointmentNotFoundException(
						"Appointment not found with id: " + command.appointmentId()));

		Appointment.validateTimeRange(command.newStart(), command.newEnd());
		Appointment.validateNotInPast(command.newStart(), LocalDateTime.now(clock));

		if (appointmentRepository.isOverlapping(
				command.userId(), command.newStart(), command.newEnd(), command.appointmentId())) {
			throw new AppointmentOverlapException(
				"The time slot %s – %s is already taken".formatted(command.newStart(), command.newEnd())
			);
		}
		return appointmentRepository.updateTimeSlot(
				command.appointmentId(), command.userId(), command.newStart(), command.newEnd());
	}
}
