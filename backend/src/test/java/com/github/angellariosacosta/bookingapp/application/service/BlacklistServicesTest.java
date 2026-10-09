package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.angellariosacosta.bookingapp.application.command.BlacklistCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.command.RemoveCustomerFromBlacklistCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.NoShowRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.BlacklistResult;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerBlacklistedException;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.BlacklistSource;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;

class BlacklistServicesTest {

	private static final Long USER_ID = 7L;
	private static final Long CUSTOMER_ID = 3L;
	private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");

	private final CustomerRepositoryPort customerRepository = mock(CustomerRepositoryPort.class);
	private final CustomerBlacklistRepositoryPort blacklistRepository = mock(CustomerBlacklistRepositoryPort.class);
	private final AppointmentRepositoryPort appointmentRepository = mock(AppointmentRepositoryPort.class);
	private final NoShowRepositoryPort noShowRepository = mock(NoShowRepositoryPort.class);
	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

	private BlacklistCustomerService blacklistService;
	private RemoveCustomerFromBlacklistService removeService;

	@BeforeEach
	void setUp() {
		BlacklistCustomerAction action = new BlacklistCustomerAction(blacklistRepository, appointmentRepository, clock);
		blacklistService = new BlacklistCustomerService(customerRepository, blacklistRepository, action, clock);
		removeService = new RemoveCustomerFromBlacklistService(blacklistRepository, noShowRepository, clock);
		when(blacklistRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void blacklistSavesEntryAndCancelsFutureAppointments() {
		when(customerRepository.findById(CUSTOMER_ID, USER_ID)).thenReturn(Optional.of(customer()));
		when(blacklistRepository.findByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(Optional.empty());
		when(appointmentRepository.cancelReservedFrom(anyLong(), anyLong(), any())).thenReturn(3);

		BlacklistResult result = blacklistService.blacklist(new BlacklistCustomerCommand(CUSTOMER_ID, USER_ID, " no llego "));

		assertEquals(3, result.cancelledAppointments());
		assertEquals(BlacklistSource.MANUAL, result.source());
		assertEquals("no llego", result.reason());
		ArgumentCaptor<CustomerBlacklist> entry = ArgumentCaptor.forClass(CustomerBlacklist.class);
		verify(blacklistRepository).save(entry.capture());
		assertEquals(USER_ID, entry.getValue().getUserId());
		verify(appointmentRepository).cancelReservedFrom(CUSTOMER_ID, USER_ID, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
	}

	@Test
	void blacklistIsIdempotentAndKeepsOriginalEntry() {
		CustomerBlacklist existing = CustomerBlacklist.autoNoShow(CUSTOMER_ID, USER_ID, 3, NOW);
		when(customerRepository.findById(CUSTOMER_ID, USER_ID)).thenReturn(Optional.of(customer()));
		when(blacklistRepository.findByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(Optional.of(existing));

		BlacklistResult result = blacklistService.blacklist(new BlacklistCustomerCommand(CUSTOMER_ID, USER_ID, "otro"));

		assertEquals(BlacklistSource.AUTO_NO_SHOW, result.source());
		assertEquals(0, result.cancelledAppointments());
		verify(blacklistRepository, never()).save(any());
		verify(appointmentRepository, never()).cancelReservedFrom(anyLong(), anyLong(), any());
	}

	@Test
	void blacklistRejectsCustomerOutsideTenant() {
		when(customerRepository.findById(CUSTOMER_ID, USER_ID)).thenReturn(Optional.empty());

		assertThrows(CustomerNotFoundException.class,
				() -> blacklistService.blacklist(new BlacklistCustomerCommand(CUSTOMER_ID, USER_ID, null)));
		verify(blacklistRepository, never()).save(any());
	}

	@Test
	void removeDeletesEntryAndForgivesActiveNoShows() {
		when(blacklistRepository.deleteByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(true);

		removeService.remove(new RemoveCustomerFromBlacklistCommand(CUSTOMER_ID, USER_ID));

		verify(noShowRepository).clearByCustomerId(CUSTOMER_ID, USER_ID, NOW);
	}

	@Test
	void removeFailsWhenCustomerWasNotBlacklisted() {
		when(blacklistRepository.deleteByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(false);

		assertThrows(CustomerNotFoundException.class,
				() -> removeService.remove(new RemoveCustomerFromBlacklistCommand(CUSTOMER_ID, USER_ID)));
		verify(noShowRepository, never()).clearByCustomerId(anyLong(), anyLong(), any());
	}

	@Test
	void guardRejectsBlacklistedCustomerOnlyWithinTenant() {
		CustomerBlacklistGuard guard = new CustomerBlacklistGuard(blacklistRepository);
		when(blacklistRepository.existsByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(true);

		assertThrows(CustomerBlacklistedException.class, () -> guard.ensureNotBlacklisted(CUSTOMER_ID, USER_ID));
		guard.ensureNotBlacklisted(CUSTOMER_ID, 99L);
	}

	private Customer customer() {
		return Customer.builder().id(CUSTOMER_ID).userId(USER_ID).build();
	}
}
