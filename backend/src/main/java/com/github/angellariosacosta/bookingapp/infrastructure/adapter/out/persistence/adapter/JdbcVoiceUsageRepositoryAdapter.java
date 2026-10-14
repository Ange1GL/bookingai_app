package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.VoiceUsageRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * Contadores de dictado con SQL nativo (JdbcClient) en vez de JPA: el límite se aplica en una sola
 * sentencia {@code INSERT ... ON CONFLICT DO UPDATE ... WHERE count < limit}, atómica en PostgreSQL,
 * así que peticiones simultáneas no pueden superar la cuota. Una fila afectada = uso consumido.
 */
@Component
@RequiredArgsConstructor
public class JdbcVoiceUsageRepositoryAdapter implements VoiceUsageRepositoryPort {

	private static final String CONSUME_DAILY = """
			INSERT INTO voice_usage_daily (user_id, usage_date, request_count) VALUES (:userId, :day, 1)
			ON CONFLICT (user_id, usage_date)
			DO UPDATE SET request_count = voice_usage_daily.request_count + 1
			WHERE voice_usage_daily.request_count < :limit
			""";

	private static final String CONSUME_MONTHLY = """
			INSERT INTO voice_usage_monthly (usage_month, request_count) VALUES (:month, 1)
			ON CONFLICT (usage_month)
			DO UPDATE SET request_count = voice_usage_monthly.request_count + 1
			WHERE voice_usage_monthly.request_count < :limit
			""";

	private static final String REFUND_DAILY = """
			UPDATE voice_usage_daily SET request_count = request_count - 1
			WHERE user_id = :userId AND usage_date = :day AND request_count > 0
			""";

	private static final String DAILY_COUNT = """
			SELECT COALESCE(MAX(request_count), 0) FROM voice_usage_daily
			WHERE user_id = :userId AND usage_date = :day
			""";

	private final JdbcClient jdbcClient;

	@Override
	public boolean tryConsumeDaily(Long userId, LocalDate day, int limit) {
		return jdbcClient.sql(CONSUME_DAILY)
				.param("userId", userId)
				.param("day", day)
				.param("limit", limit)
				.update() == 1;
	}

	@Override
	public boolean tryConsumeMonthly(LocalDate firstDayOfMonth, int limit) {
		return jdbcClient.sql(CONSUME_MONTHLY)
				.param("month", firstDayOfMonth)
				.param("limit", limit)
				.update() == 1;
	}

	@Override
	public void refundDaily(Long userId, LocalDate day) {
		jdbcClient.sql(REFUND_DAILY)
				.param("userId", userId)
				.param("day", day)
				.update();
	}

	@Override
	public int dailyCount(Long userId, LocalDate day) {
		return jdbcClient.sql(DAILY_COUNT)
				.param("userId", userId)
				.param("day", day)
				.query(Integer.class)
				.single();
	}
}
