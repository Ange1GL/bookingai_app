package com.github.angellariosacosta.bookingapp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class OneTimeCodeTest {

	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
	private static final Duration TTL = Duration.ofMinutes(30);

	private OneTimeCode issued() {
		return OneTimeCode.issue(7L, OneTimeCodePurpose.ACCOUNT_ACTIVATION, "hash", NOW, TTL);
	}

	@Test
	void issuedCodeIsUsableUntilItExpires() {
		OneTimeCode code = issued();

		assertEquals(NOW.plus(TTL), code.getExpiresAt());
		assertTrue(code.isUsable(NOW));
		assertTrue(code.isUsable(NOW.plus(TTL).minusSeconds(1)));
		assertFalse(code.isUsable(NOW.plus(TTL)));
	}

	@Test
	void consumedCodeIsNeverUsableAgain() {
		OneTimeCode code = issued();

		code.consume(NOW.plusSeconds(5));

		assertFalse(code.isUsable(NOW.plusSeconds(6)));
		assertEquals(NOW.plusSeconds(5), code.getConsumedAt());
	}

	@Test
	void consumingTwiceKeepsTheFirstInstant() {
		OneTimeCode code = issued();

		code.consume(NOW.plusSeconds(5));
		code.consume(NOW.plusSeconds(60));

		assertEquals(NOW.plusSeconds(5), code.getConsumedAt());
	}

	@Test
	void reachingTheMaximumOfFailuresInvalidatesTheCode() {
		OneTimeCode code = issued();

		for (int attempt = 1; attempt < OneTimeCode.MAX_FAILED_ATTEMPTS; attempt++) {
			code.registerFailure(NOW);
			assertTrue(code.isUsable(NOW), "intento " + attempt);
			assertNull(code.getConsumedAt());
		}
		code.registerFailure(NOW);

		assertFalse(code.isUsable(NOW));
		assertNotNull(code.getConsumedAt());
		assertEquals(OneTimeCode.MAX_FAILED_ATTEMPTS, code.getFailedAttempts());
	}

	@Test
	void purposeIdsRoundTripAndUnknownIdsAreRejected() {
		for (OneTimeCodePurpose purpose : OneTimeCodePurpose.values()) {
			assertEquals(purpose, OneTimeCodePurpose.fromId(purpose.getId()));
		}
		assertThrows(IllegalStateException.class, () -> OneTimeCodePurpose.fromId(99));
	}
}
