package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.result.RateLimitDecision;

class InMemoryCustomerRateLimitAdapterTest {

	private static final String PHONE = "5215512345678";
	private static final String OTHER_PHONE = "5215598765432";
	private static final Instant START = Instant.parse("2026-10-14T12:00:00Z");

	private final MutableClock clock = new MutableClock(START);
	private final InMemoryCustomerRateLimitAdapter adapter = new InMemoryCustomerRateLimitAdapter(clock);

	private void consume(String phone, int times) {
		for (int i = 0; i < times; i++) {
			adapter.tryAcquire(phone);
		}
	}

	@DisplayName("Should allow messages up to the limit")
	@Test
	void shouldAllowUpToLimit() {
		for (int i = 0; i < InMemoryCustomerRateLimitAdapter.MAX_MESSAGES_PER_WINDOW; i++) {
			assertEquals(RateLimitDecision.ALLOWED, adapter.tryAcquire(PHONE));
		}
	}

	@DisplayName("Should notify once and then drop silently beyond the limit")
	@Test
	void shouldNotifyOnceThenDropSilently() {
		consume(PHONE, InMemoryCustomerRateLimitAdapter.MAX_MESSAGES_PER_WINDOW);

		assertEquals(RateLimitDecision.REJECTED_NOTIFY, adapter.tryAcquire(PHONE));
		assertEquals(RateLimitDecision.REJECTED_SILENT, adapter.tryAcquire(PHONE));
		assertEquals(RateLimitDecision.REJECTED_SILENT, adapter.tryAcquire(PHONE));
	}

	@DisplayName("Should not affect other phones")
	@Test
	void shouldIsolatePhones() {
		consume(PHONE, InMemoryCustomerRateLimitAdapter.MAX_MESSAGES_PER_WINDOW + 1);

		assertEquals(RateLimitDecision.ALLOWED, adapter.tryAcquire(OTHER_PHONE));
	}

	@DisplayName("Should allow again once the window has passed")
	@Test
	void shouldResetAfterWindow() {
		consume(PHONE, InMemoryCustomerRateLimitAdapter.MAX_MESSAGES_PER_WINDOW + 1);

		clock.advance(InMemoryCustomerRateLimitAdapter.WINDOW);

		assertEquals(RateLimitDecision.ALLOWED, adapter.tryAcquire(PHONE));
	}

	static final class MutableClock extends Clock {
		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(java.time.Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
