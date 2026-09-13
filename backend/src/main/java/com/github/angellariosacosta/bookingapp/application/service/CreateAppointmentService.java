package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreateAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAppointmentService implements CreateAppointmentUseCase {

	private final AppointmentRepository appointmentRepository;
	private final CustomerRepository customerRepository;

	@Override
	@Transactional
	public Appointment create(CreateAppointmentCommand command) {
		Customer customer = customerRepository.findById(command.customerId())
				.orElseThrow(() -> new CustomerNotFoundException(
						"Customer not found with id: " + command.customerId()));

		boolean isOverlapping = appointmentRepository.isOverlapping(command.startTime(), command.endTime());
		if (isOverlapping) {
			throw new AppointmentOverlapException("There is an appointment previously with same time");
		}

		Appointment appointment = Appointment.createNew(command.startTime(), command.endTime(), customer, command.userId());
		return appointmentRepository.save(appointment);
	}
}
