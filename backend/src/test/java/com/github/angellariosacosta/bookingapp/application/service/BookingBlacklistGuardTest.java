package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.command.CreateAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerBlacklistedException;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

/** Las reservas (REST y tools de AI) deben cortarse cuando el cliente está en lista negra. */
class BookingBlacklistGuardTest {

	private static final Long USER_ID = 7L;
	private static final Long CUSTOMER_ID = 3L;
	private static final LocalDateTime START = LocalDateTime.of(2026, 10, 10, 10, 0);
	private static final LocalDateTime END = START.plusMinutes(30);

	private final CustomerRepositoryPort customerRepository = mock(CustomerRepositoryPort.class);
	private final AppointmentRepositoryPort appointmentRepository = mock(AppointmentRepositoryPort.class);
	private final PriceCatalogRepositoryPort priceCatalogRepository = mock(PriceCatalogRepositoryPort.class);
	private final CustomerBlacklistGuard guard = mock(CustomerBlacklistGuard.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneOffset.UTC);

	@Test
	void createAppointmentIsRejectedForBlacklistedCustomer() {
		CreateAppointmentService service = new CreateAppointmentService(
				appointmentRepository, customerRepository, priceCatalogRepository, guard, clock);
		when(customerRepository.findById(CUSTOMER_ID, USER_ID)).thenReturn(Optional.of(customer()));
		doThrow(new CustomerBlacklistedException("blacklisted")).when(guard).ensureNotBlacklisted(CUSTOMER_ID, USER_ID);

		assertThrows(CustomerBlacklistedException.class,
				() -> service.create(new CreateAppointmentCommand(START, END, CUSTOMER_ID, USER_ID, 1)));
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void bookAppointmentIsRejectedForBlacklistedCustomer() {
		BookAppointmentService service = new BookAppointmentService(
				customerRepository, appointmentRepository, priceCatalogRepository, guard, clock);
		when(customerRepository.findByPhone("555", USER_ID)).thenReturn(Optional.of(customer()));
		doThrow(new CustomerBlacklistedException("blacklisted")).when(guard).ensureNotBlacklisted(CUSTOMER_ID, USER_ID);

		assertThrows(CustomerBlacklistedException.class,
				() -> service.book(new BookAppointmentCommand("Ana", "555", START, END, USER_ID, 1)));
		verify(appointmentRepository, never()).save(any());
	}

	private Customer customer() {
		return Customer.builder().id(CUSTOMER_ID).userId(USER_ID).phone("555").name("Ana").build();
	}
}
