package com.github.angellariosacosta.bookingapp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.NoShowNotAllowedException;

class CustomerBlacklistTest {

	private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
	private static final LocalDateTime LOCAL_NOW = LocalDateTime.of(2026, 10, 9, 12, 0);

	@Test
	void entryRecordsNoShowCountAsReason() {
		CustomerBlacklist entry = CustomerBlacklist.forNoShows(1L, 2L, 3, NOW);

		assertEquals("Auto: 3 inasistencias", entry.getReason());
		assertEquals(NOW, entry.getCreatedAt());
	}

	@Test
	void ownersAreRequired() {
		assertThrows(InvalidFieldException.class, () -> CustomerBlacklist.forNoShows(null, 2L, 3, NOW));
		assertThrows(InvalidFieldException.class, () -> CustomerBlacklist.forNoShows(1L, null, 3, NOW));
	}

	@Test
	void noShowKeepsStrippedReason() {
		NoShow noShow = NoShow.register(1L, 10L, 2L, "  no llego  ", NOW);

		assertEquals("no llego", noShow.getReason());
		assertEquals(10L, noShow.getAppointmentId());
	}

	@Test
	void noShowBlankReasonBecomesNull() {
		assertNull(NoShow.register(1L, 10L, 2L, "   ", NOW).getReason());
		assertNull(NoShow.register(1L, 10L, 2L, null, NOW).getReason());
	}

	@Test
	void noShowReasonAcceptsExactlyTheMaximumLength() {
		String max = "x".repeat(NoShow.MAX_REASON_LENGTH);
		assertEquals(250, NoShow.register(1L, 10L, 2L, max, NOW).getReason().length());

		String tooLong = "x".repeat(NoShow.MAX_REASON_LENGTH + 1);
		assertThrows(InvalidFieldException.class, () -> NoShow.register(1L, 10L, 2L, tooLong, NOW));
	}

	@Test
	void noShowRequiresOwners() {
		assertThrows(InvalidFieldException.class, () -> NoShow.register(null, 10L, 2L, null, NOW));
		assertThrows(InvalidFieldException.class, () -> NoShow.register(1L, null, 2L, null, NOW));
		assertThrows(InvalidFieldException.class, () -> NoShow.register(1L, 10L, null, null, NOW));
	}

	@Test
	void policyReachesThresholdInclusive() {
		BlacklistPolicy policy = new BlacklistPolicy(3);

		assertFalse(policy.isReached(2));
		assertTrue(policy.isReached(3));
		assertTrue(policy.isReached(4));
	}

	@Test
	void policyWithNonPositiveThresholdNeverBlocks() {
		assertFalse(new BlacklistPolicy(0).isReached(100));
		assertFalse(new BlacklistPolicy(-1).isReached(100));
	}

	@Test
	void noShowRequiresStartedReservedAppointment() {
		Appointment started = appointment(StatusAppointment.RESERVED, LOCAL_NOW.minusHours(1));
		started.ensureNoShowRegistrable(LOCAL_NOW);

		Appointment future = appointment(StatusAppointment.RESERVED, LOCAL_NOW.plusHours(1));
		assertThrows(NoShowNotAllowedException.class, () -> future.ensureNoShowRegistrable(LOCAL_NOW));

		Appointment cancelled = appointment(StatusAppointment.CANCELLED, LOCAL_NOW.minusHours(1));
		assertThrows(NoShowNotAllowedException.class, () -> cancelled.ensureNoShowRegistrable(LOCAL_NOW));
	}

	private Appointment appointment(StatusAppointment status, LocalDateTime start) {
		Customer customer = Customer.builder().id(1L).userId(2L).build();
		PriceCatalog service = new PriceCatalog(1, 100, "Corte", 2L, true);
		return Appointment.reconstitute(10L, start, start.plusMinutes(30), customer, status, 2L, service);
	}
}
