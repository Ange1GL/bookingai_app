package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.command.RemoveCustomerFromBlacklistCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.NoShowRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerBlacklistedException;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;

class BlacklistServicesTest {

	private static final Long USER_ID = 7L;
	private static final Long CUSTOMER_ID = 3L;
	private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");

	private final CustomerBlacklistRepositoryPort blacklistRepository = mock(CustomerBlacklistRepositoryPort.class);
	private final NoShowRepositoryPort noShowRepository = mock(NoShowRepositoryPort.class);
	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private final RemoveCustomerFromBlacklistService removeService =
			new RemoveCustomerFromBlacklistService(blacklistRepository, noShowRepository, clock);

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
}
