package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.memory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.ProcessedMessagePort;

import lombok.RequiredArgsConstructor;

/**
 * Ids ya vistos, en memoria (demo): acotado y con caducidad; se pierde al reiniciar y no se comparte entre instancias
 * (con varias instancias haría falta Redis o una tabla con clave única).
 */
@Component
@RequiredArgsConstructor
public class InMemoryProcessedMessageAdapter implements ProcessedMessagePort {

	static final int MAX_TRACKED_MESSAGES = 10_000;
	static final Duration RETENTION = Duration.ofHours(24);
	private static final float LOAD_FACTOR = 0.75f;

	private final Clock clock;

	private final Map<String, Instant> seen = new LinkedHashMap<>(16, LOAD_FACTOR) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Instant> eldest) {
			return size() > MAX_TRACKED_MESSAGES;
		}
	};

	@Override
	public synchronized boolean markIfNew(String messageId) {
		Instant now = clock.instant();
		Instant previous = seen.get(messageId);
		if (previous != null && previous.plus(RETENTION).isAfter(now)) {
			return false;
		}
		seen.put(messageId, now);
		return true;
	}
}
