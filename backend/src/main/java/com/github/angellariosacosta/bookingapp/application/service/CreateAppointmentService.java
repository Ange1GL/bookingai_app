package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.CreateAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.PriceCatalogNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAppointmentService implements CreateAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepository;
	private final CustomerRepositoryPort customerRepository;
	private final PriceCatalogRepositoryPort priceCatalogRepository;
	private final CustomerBlacklistGuard blacklistGuard;
	private final Clock clock;

	@Override
	@Transactional
	public Appointment create(CreateAppointmentCommand command) {
		Customer customer = customerRepository.findById(command.customerId(), command.userId())
				.orElseThrow(() -> new CustomerNotFoundException(
						"Customer not found with id: " + command.customerId()));
		blacklistGuard.ensureNotBlacklisted(customer.getId(), command.userId());

		boolean isOverlapping = appointmentRepository.isOverlapping(command.userId(), command.startTime(), command.endTime());
		if (isOverlapping) {
			throw new AppointmentOverlapException("There is an appointment previously with same time");
		}

		PriceCatalog priceCatalog = priceCatalogRepository.findById(command.priceCatalogId(), command.userId())
				.orElseThrow(() -> new PriceCatalogNotFoundException(
						"Price catalog not found with id: " + command.priceCatalogId()));

		Appointment appointment = Appointment.createNew(command.startTime(), command.endTime(), customer, command.userId(), priceCatalog, LocalDateTime.now(clock));
		return appointmentRepository.save(appointment);
	}
}
