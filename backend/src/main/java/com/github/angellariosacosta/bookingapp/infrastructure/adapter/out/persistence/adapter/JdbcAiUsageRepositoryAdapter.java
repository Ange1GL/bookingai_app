package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AiUsageRepositoryPort;

import lombok.RequiredArgsConstructor;

/** Contadores de tokens con SQL nativo (JdbcClient): el upsert suma de forma atómica sin carreras. */
@Component
@RequiredArgsConstructor
public class JdbcAiUsageRepositoryAdapter implements AiUsageRepositoryPort {

	private static final String ADD_USER_USAGE = """
			INSERT INTO ai_usage_daily (user_id, usage_date, tokens_used, request_count) VALUES (:userId, :day, :tokens, 1)
			ON CONFLICT (user_id, usage_date)
			DO UPDATE SET tokens_used = ai_usage_daily.tokens_used + :tokens,
			              request_count = ai_usage_daily.request_count + 1
			""";

	private static final String ADD_GLOBAL_USAGE = """
			INSERT INTO ai_usage_global_daily (usage_date, tokens_used) VALUES (:day, :tokens)
			ON CONFLICT (usage_date)
			DO UPDATE SET tokens_used = ai_usage_global_daily.tokens_used + :tokens
			""";

	private static final String USER_USED = """
			SELECT COALESCE(MAX(tokens_used), 0) FROM ai_usage_daily
			WHERE user_id = :userId AND usage_date = :day
			""";

	private static final String GLOBAL_USED = """
			SELECT COALESCE(MAX(tokens_used), 0) FROM ai_usage_global_daily WHERE usage_date = :day
			""";

	private final JdbcClient jdbcClient;

	@Override
	public long usedByUser(Long userId, LocalDate day) {
		return jdbcClient.sql(USER_USED).param("userId", userId).param("day", day).query(Long.class).single();
	}

	@Override
	public long usedGlobally(LocalDate day) {
		return jdbcClient.sql(GLOBAL_USED).param("day", day).query(Long.class).single();
	}

	@Override
	public void addUsage(Long userId, LocalDate day, long tokens) {
		jdbcClient.sql(ADD_USER_USAGE).param("userId", userId).param("day", day).param("tokens", tokens).update();
		jdbcClient.sql(ADD_GLOBAL_USAGE).param("day", day).param("tokens", tokens).update();
	}
}
