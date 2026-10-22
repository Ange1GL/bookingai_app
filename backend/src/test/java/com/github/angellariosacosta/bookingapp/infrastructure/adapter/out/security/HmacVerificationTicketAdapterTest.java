package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class HmacVerificationTicketAdapterTest {

	private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
	private static final long USER_ID = 4L;
	private static final long CODE_ID = 9L;

	private final HmacVerificationTicketAdapter adapter = adapterWithSecret('a');

	private static HmacVerificationTicketAdapter adapterWithSecret(char fill) {
		byte[] secret = new byte[32];
		java.util.Arrays.fill(secret, (byte) fill);
		return new HmacVerificationTicketAdapter(new SecretKeySpec(secret, "HmacSHA256"));
	}

	private String ticket() {
		return adapter.issue(USER_ID, CODE_ID, NOW.plusSeconds(60));
	}

	@Test
	void acceptsItsOwnTicketForTheSameUserAndCode() {
		assertTrue(adapter.isValid(ticket(), USER_ID, CODE_ID, NOW));
	}

	@Test
	void rejectsAnExpiredTicket() {
		assertFalse(adapter.isValid(ticket(), USER_ID, CODE_ID, NOW.plusSeconds(60)));
	}

	@Test
	void rejectsAnotherUserOrAnotherCode() {
		assertFalse(adapter.isValid(ticket(), USER_ID + 1, CODE_ID, NOW));
		assertFalse(adapter.isValid(ticket(), USER_ID, CODE_ID + 1, NOW));
	}

	@Test
	void rejectsATicketSignedWithAnotherSecret() {
		assertFalse(adapterWithSecret('b').isValid(ticket(), USER_ID, CODE_ID, NOW));
	}

	@Test
	void rejectsATamperedPayload() {
		String[] parts = ticket().split("\\.");
		String forged = adapter.issue(USER_ID, CODE_ID + 1, NOW.plusSeconds(60)).split("\\.")[0] + "." + parts[1];

		assertFalse(adapter.isValid(forged, USER_ID, CODE_ID + 1, NOW));
	}

	@Test
	void rejectsMalformedTickets() {
		assertFalse(adapter.isValid(null, USER_ID, CODE_ID, NOW));
		assertFalse(adapter.isValid("", USER_ID, CODE_ID, NOW));
		assertFalse(adapter.isValid("a.b.c", USER_ID, CODE_ID, NOW));
		assertFalse(adapter.isValid("not base64!.also not", USER_ID, CODE_ID, NOW));
	}
}
