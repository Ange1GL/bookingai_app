package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.port.out.VoiceUsageRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.VoiceQuota;

@ExtendWith(MockitoExtension.class)
class GetVoiceQuotaServiceTest {

	private static final Long USER_ID = 7L;
	private static final int DAILY_LIMIT = 40;
	private static final int MONTHLY_LIMIT = 1500;
	// 2026-10-14T03:00Z todavía es 13 de octubre en Ciudad de México (UTC-6).
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), ZoneId.of("America/Mexico_City"));
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);
	private static final LocalDate FIRST_OF_MONTH = LocalDate.of(2026, 10, 1);

	@Mock
	private VoiceUsageRepositoryPort voiceUsage;

	private GetVoiceQuotaService service;

	@BeforeEach
	void setUp() {
		service = new GetVoiceQuotaService(voiceUsage, new VoiceTranscriptionPolicy(16, 64, DAILY_LIMIT, MONTHLY_LIMIT), CLOCK);
	}

	@Test
	@DisplayName("devuelve lo usado hoy y la hora de recarga del día de negocio")
	void returnsDailyUsageAndReset() {
		when(voiceUsage.dailyCount(USER_ID, TODAY)).thenReturn(12);
		when(voiceUsage.monthlyCount(FIRST_OF_MONTH)).thenReturn(100);

		VoiceQuota quota = service.getQuota(USER_ID);

		assertEquals(28, quota.remaining());
		assertEquals(30, quota.percentUsed());
		assertFalse(quota.monthlyExhausted());
		assertEquals(OffsetDateTime.parse("2026-10-14T00:00:00-06:00"), quota.resetsAt());
	}

	@Test
	@DisplayName("con el tope mensual global agotado no quedan dictados aunque el usuario tenga del día")
	void monthlyExhaustedLeavesNothing() {
		when(voiceUsage.dailyCount(USER_ID, TODAY)).thenReturn(2);
		when(voiceUsage.monthlyCount(FIRST_OF_MONTH)).thenReturn(MONTHLY_LIMIT);

		VoiceQuota quota = service.getQuota(USER_ID);

		assertTrue(quota.monthlyExhausted());
		assertEquals(0, quota.remaining());
		assertEquals(100, quota.percentUsed());
	}
}
