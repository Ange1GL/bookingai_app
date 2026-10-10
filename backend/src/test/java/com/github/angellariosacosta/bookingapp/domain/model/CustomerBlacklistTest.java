package com.github.angellariosacosta.bookingapp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

class CustomerBlacklistTest {

	private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

	@Test
	void createKeepsStrippedReason() {
		CustomerBlacklist entry = CustomerBlacklist.create(1L, 2L, "  no llego  ", NOW);

		assertEquals("no llego", entry.getReason());
		assertEquals(NOW, entry.getCreatedAt());
	}

	@Test
	void blankReasonBecomesNull() {
		assertNull(CustomerBlacklist.create(1L, 2L, "   ", NOW).getReason());
		assertNull(CustomerBlacklist.create(1L, 2L, null, NOW).getReason());
	}

	@Test
	void reasonAcceptsExactlyTheMaximumLength() {
		String max = "x".repeat(CustomerBlacklist.MAX_REASON_LENGTH);
		assertEquals(250, CustomerBlacklist.create(1L, 2L, max, NOW).getReason().length());

		String tooLong = "x".repeat(CustomerBlacklist.MAX_REASON_LENGTH + 1);
		assertThrows(InvalidFieldException.class, () -> CustomerBlacklist.create(1L, 2L, tooLong, NOW));
	}

	@Test
	void ownersAreRequired() {
		assertThrows(InvalidFieldException.class, () -> CustomerBlacklist.create(null, 2L, null, NOW));
		assertThrows(InvalidFieldException.class, () -> CustomerBlacklist.create(1L, null, null, NOW));
	}
}
