package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BookAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookAppointmentService implements BookAppointmentUseCase {

	private final CustomerRepositoryPort customerRepository;
	private final AppointmentRepositoryPort appointmentRepository;
	private final Clock clock;

	@Override
	@Transactional
	public Appointment book(BookAppointmentCommand command) {
		Appointment.validateNotInPast(command.startTime(), LocalDateTime.now(clock));

		Customer customer = customerRepository.findByPhone(command.phone(), command.userId())
				.orElseGet(() -> customerRepository.save(
						Customer.builder()
								.name(command.name())
								.phone(command.phone())
								.userId(command.userId())
								.build()
				));

		if (appointmentRepository.isOverlapping(command.userId(),command.startTime(), command.endTime())) {
			throw new AppointmentOverlapException("There is an appointment previously with same time");
		}

		Appointment appointment = Appointment.createNew(command.startTime(), command.endTime(), customer, command.userId());
		return appointmentRepository.save(appointment);
	}
}
