package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BookAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.PriceCatalogNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookAppointmentService implements BookAppointmentUseCase {

	private final CustomerRepositoryPort customerRepository;
	private final AppointmentRepositoryPort appointmentRepository;
	private final PriceCatalogRepositoryPort priceCatalogRepository;
	private final CustomerBlacklistGuard blacklistGuard;
	private final Clock clock;

	@Override
	@Transactional
	public Appointment book(BookAppointmentCommand command) {
		Customer customer = customerRepository.findByPhone(command.phone(), command.userId())
				.orElseGet(() -> customerRepository.save(
						Customer.builder()
								.name(command.name())
								.phone(command.phone())
								.userId(command.userId())
								.build()
				));
		blacklistGuard.ensureNotBlacklisted(customer.getId(), command.userId());
		if (appointmentRepository.isOverlapping(command.userId(),command.startTime(), command.endTime())) {
			throw new AppointmentOverlapException("There is an appointment previously with same time");
		}

		PriceCatalog priceCatalog = priceCatalogRepository.findById(command.priceCatalogId(), command.userId())
				.orElseThrow(() -> new PriceCatalogNotFoundException(
						"Price catalog not found with id: " + command.priceCatalogId()));

		Appointment appointment = Appointment.createNew(command.startTime(), command.endTime(), customer, command.userId(), priceCatalog, LocalDateTime.now(clock));
		return appointmentRepository.save(appointment);
	}
}
