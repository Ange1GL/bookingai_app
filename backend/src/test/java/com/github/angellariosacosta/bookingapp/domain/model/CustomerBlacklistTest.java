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
	void manualEntryKeepsStrippedReason() {
		CustomerBlacklist entry = CustomerBlacklist.manual(1L, 2L, "  no llego  ", NOW);

		assertEquals("no llego", entry.getReason());
		assertEquals(BlacklistSource.MANUAL, entry.getSource());
		assertEquals(NOW, entry.getCreatedAt());
	}

	@Test
	void blankReasonBecomesNull() {
		assertNull(CustomerBlacklist.manual(1L, 2L, "   ", NOW).getReason());
		assertNull(CustomerBlacklist.manual(1L, 2L, null, NOW).getReason());
	}

	@Test
	void tooLongReasonIsRejected() {
		String tooLong = "x".repeat(CustomerBlacklist.MAX_REASON_LENGTH + 1);
		assertThrows(InvalidFieldException.class, () -> CustomerBlacklist.manual(1L, 2L, tooLong, NOW));
	}

	@Test
	void ownersAreRequired() {
		assertThrows(InvalidFieldException.class, () -> CustomerBlacklist.manual(null, 2L, "x", NOW));
		assertThrows(InvalidFieldException.class, () -> CustomerBlacklist.manual(1L, null, "x", NOW));
	}

	@Test
	void autoEntryRecordsNoShowCountAsReason() {
		CustomerBlacklist entry = CustomerBlacklist.autoNoShow(1L, 2L, 3, NOW);

		assertEquals(BlacklistSource.AUTO_NO_SHOW, entry.getSource());
		assertEquals("Auto: 3 inasistencias", entry.getReason());
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
