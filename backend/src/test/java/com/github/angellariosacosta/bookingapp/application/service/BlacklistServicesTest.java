package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;

import com.github.angellariosacosta.bookingapp.application.command.BlacklistCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.command.RemoveCustomerFromBlacklistCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerBlacklistedException;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;

class BlacklistServicesTest {

	private static final Long USER_ID = 7L;
	private static final Long CUSTOMER_ID = 3L;
	private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");

	private final CustomerBlacklistRepositoryPort blacklistRepository = mock(CustomerBlacklistRepositoryPort.class);
	private final CustomerRepositoryPort customerRepository = mock(CustomerRepositoryPort.class);
	private final AppointmentRepositoryPort appointmentRepository = mock(AppointmentRepositoryPort.class);
	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private final BlacklistCustomerService blacklistService = new BlacklistCustomerService(
			customerRepository, blacklistRepository,
			new BlacklistCustomerAction(blacklistRepository, appointmentRepository, clock), clock);
	private final RemoveCustomerFromBlacklistService removeService =
			new RemoveCustomerFromBlacklistService(blacklistRepository);

	@Test
	void blacklistSavesEntryAndCancelsFutureReservations() {
		when(customerRepository.findById(CUSTOMER_ID, USER_ID))
				.thenReturn(Optional.of(Customer.builder().id(CUSTOMER_ID).userId(USER_ID).build()));
		when(blacklistRepository.existsByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(false);
		when(blacklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		blacklistService.blacklist(new BlacklistCustomerCommand(CUSTOMER_ID, USER_ID, " no llego "));

		ArgumentCaptor<CustomerBlacklist> saved = ArgumentCaptor.forClass(CustomerBlacklist.class);
		verify(blacklistRepository).save(saved.capture());
		assertEquals("no llego", saved.getValue().getReason());
		assertEquals(NOW, saved.getValue().getCreatedAt());
		verify(appointmentRepository).cancelReservedFrom(
				CUSTOMER_ID, USER_ID, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
	}

	@Test
	void blacklistIsIdempotentWhenAlreadyListed() {
		when(customerRepository.findById(CUSTOMER_ID, USER_ID))
				.thenReturn(Optional.of(Customer.builder().id(CUSTOMER_ID).userId(USER_ID).build()));
		when(blacklistRepository.existsByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(true);

		blacklistService.blacklist(new BlacklistCustomerCommand(CUSTOMER_ID, USER_ID, null));

		verify(blacklistRepository, never()).save(any());
	}

	@Test
	void blacklistFailsForCustomerOutsideTenant() {
		when(customerRepository.findById(CUSTOMER_ID, USER_ID)).thenReturn(Optional.empty());

		assertThrows(CustomerNotFoundException.class,
				() -> blacklistService.blacklist(new BlacklistCustomerCommand(CUSTOMER_ID, USER_ID, null)));
		verify(blacklistRepository, never()).save(any());
	}

	@Test
	void removeDeletesEntry() {
		when(blacklistRepository.deleteByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(true);

		removeService.remove(new RemoveCustomerFromBlacklistCommand(CUSTOMER_ID, USER_ID));

		verify(blacklistRepository).deleteByCustomerId(CUSTOMER_ID, USER_ID);
	}

	@Test
	void removeFailsWhenCustomerWasNotBlacklisted() {
		when(blacklistRepository.deleteByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(false);

		assertThrows(CustomerNotFoundException.class,
				() -> removeService.remove(new RemoveCustomerFromBlacklistCommand(CUSTOMER_ID, USER_ID)));
	}

	@Test
	void guardRejectsBlacklistedCustomerOnlyWithinTenant() {
		CustomerBlacklistGuard guard = new CustomerBlacklistGuard(blacklistRepository);
		when(blacklistRepository.existsByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(true);

		assertThrows(CustomerBlacklistedException.class, () -> guard.ensureNotBlacklisted(CUSTOMER_ID, USER_ID));
		guard.ensureNotBlacklisted(CUSTOMER_ID, 99L);
	}
}
