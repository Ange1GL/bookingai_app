package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.out.AiUsageRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.AiCredits;
import com.github.angellariosacosta.bookingapp.domain.exception.AiQuotaExceededException;

import lombok.RequiredArgsConstructor;

/** Reglas de créditos de AI compartidas por el chat y la consulta de saldo. El día es el de negocio ({@link Clock}). */
@Component
@RequiredArgsConstructor
public class AiCreditsTracker {

	static final String USER_LIMIT_MESSAGE = "Agotaste tus créditos de AI de hoy";
	static final String GLOBAL_LIMIT_MESSAGE = "El asistente alcanzó su límite de uso de hoy";

	private final AiUsageRepositoryPort usage;
	private final AiCreditsPolicy policy;
	private final Clock clock;

	public AiCredits snapshot(Long userId) {
		LocalDate today = LocalDate.now(clock);
		OffsetDateTime resetsAt = today.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
		return new AiCredits(policy.dailyTokenLimitPerUser(), usage.usedByUser(userId, today), resetsAt);
	}

	/** Pre-chequeo: el costo real se conoce hasta después de la respuesta, así que solo se exige saldo disponible. */
	public void assertAvailable(Long userId) {
		if (snapshot(userId).exhausted()) {
			throw new AiQuotaExceededException(USER_LIMIT_MESSAGE);
		}
		if (usage.usedGlobally(LocalDate.now(clock)) >= policy.globalDailyTokenLimit()) {
			throw new AiQuotaExceededException(GLOBAL_LIMIT_MESSAGE);
		}
	}

	@Transactional
	public void record(Long userId, long tokens) {
		usage.addUsage(userId, LocalDate.now(clock), Math.max(0, tokens));
	}
}
