package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BookAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.port.out.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookAppointmentService implements BookAppointmentUseCase {

	private final CustomerRepository customerRepository;
	private final AppointmentRepository appointmentRepository;

	@Override
	@Transactional
	public Appointment book(BookAppointmentCommand command) {
		Customer customer = customerRepository.findByPhone(command.phone())
				.orElseGet(() -> customerRepository.save(
						Customer.builder()
								.name(command.name())
								.phone(command.phone())
								.build()
				));

		if (appointmentRepository.isOverlapping(command.startTime(), command.endTime())) {
			throw new AppointmentOverlapException("There is an appointment previously with same time");
		}

		Appointment appointment = Appointment.createNew(command.startTime(), command.endTime(), customer);
		return appointmentRepository.save(appointment);
	}
}
