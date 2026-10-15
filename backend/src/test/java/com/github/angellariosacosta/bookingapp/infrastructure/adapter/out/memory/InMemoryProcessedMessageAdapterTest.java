package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.memory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.memory.InMemoryCustomerRateLimitAdapterTest.MutableClock;

class InMemoryProcessedMessageAdapterTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-14T12:00:00Z"));
	private final InMemoryProcessedMessageAdapter adapter = new InMemoryProcessedMessageAdapter(clock);

	@DisplayName("Should accept a message id the first time and reject it afterwards")
	@Test
	void shouldRejectRepeatedId() {
		assertTrue(adapter.markIfNew("wamid.1"));
		assertFalse(adapter.markIfNew("wamid.1"));
	}

	@DisplayName("Should accept different ids")
	@Test
	void shouldAcceptDifferentIds() {
		assertTrue(adapter.markIfNew("wamid.1"));
		assertTrue(adapter.markIfNew("wamid.2"));
	}

	@DisplayName("Should accept an id again once the retention has expired")
	@Test
	void shouldForgetAfterRetention() {
		adapter.markIfNew("wamid.1");

		clock.advance(InMemoryProcessedMessageAdapter.RETENTION);

		assertTrue(adapter.markIfNew("wamid.1"));
	}

	@DisplayName("Should evict the oldest ids beyond the maximum tracked")
	@Test
	void shouldEvictOldestBeyondMax() {
		adapter.markIfNew("oldest");
		for (int i = 0; i < InMemoryProcessedMessageAdapter.MAX_TRACKED_MESSAGES; i++) {
			adapter.markIfNew("wamid." + i);
		}

		assertTrue(adapter.markIfNew("oldest"));
	}
}
