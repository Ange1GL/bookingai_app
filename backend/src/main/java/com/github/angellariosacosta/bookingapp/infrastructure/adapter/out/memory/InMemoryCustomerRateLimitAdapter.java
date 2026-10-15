package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.memory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRateLimitPort;
import com.github.angellariosacosta.bookingapp.application.result.RateLimitDecision;

import lombok.RequiredArgsConstructor;

/** Ventana fija por teléfono, en memoria (demo): {@value #MAX_MESSAGES_PER_WINDOW} mensajes por ventana. */
@Component
@RequiredArgsConstructor
public class InMemoryCustomerRateLimitAdapter implements CustomerRateLimitPort {

	static final int MAX_MESSAGES_PER_WINDOW = 10;
	static final Duration WINDOW = Duration.ofMinutes(1);
	static final int MAX_TRACKED_PHONES = 10_000;
	private static final float LOAD_FACTOR = 0.75f;

	private final Clock clock;

	private final Map<String, Window> windows = new LinkedHashMap<>(16, LOAD_FACTOR, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Window> eldest) {
			return size() > MAX_TRACKED_PHONES;
		}
	};

	@Override
	public synchronized RateLimitDecision tryAcquire(String customerPhone) {
		Instant now = clock.instant();
		Window window = windows.get(customerPhone);
		if (window == null || !window.start().plus(WINDOW).isAfter(now)) {
			window = new Window(now, 0);
		}
		Window next = new Window(window.start(), window.count() + 1);
		windows.put(customerPhone, next);
		if (next.count() <= MAX_MESSAGES_PER_WINDOW) {
			return RateLimitDecision.ALLOWED;
		}
		return next.count() == MAX_MESSAGES_PER_WINDOW + 1
				? RateLimitDecision.REJECTED_NOTIFY
				: RateLimitDecision.REJECTED_SILENT;
	}

	private record Window(Instant start, int count) {
	}
}
