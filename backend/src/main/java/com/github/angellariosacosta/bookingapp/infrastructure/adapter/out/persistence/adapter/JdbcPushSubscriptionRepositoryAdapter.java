package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.PushSubscriptionRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.PushSubscription;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JdbcPushSubscriptionRepositoryAdapter implements PushSubscriptionRepositoryPort {

	// Si el mismo navegador inicia sesión con otro usuario, el dispositivo pasa a ese usuario.
	private static final String UPSERT = """
			INSERT INTO push_subscription (user_id, endpoint, p256dh, auth, user_agent, created_at)
			VALUES (:userId, :endpoint, :p256dh, :auth, :userAgent, now())
			ON CONFLICT (endpoint)
			DO UPDATE SET user_id = EXCLUDED.user_id, p256dh = EXCLUDED.p256dh,
			              auth = EXCLUDED.auth, user_agent = EXCLUDED.user_agent
			""";

	private static final String FIND_BY_USER = """
			SELECT id, user_id, endpoint, p256dh, auth, user_agent FROM push_subscription WHERE user_id = :userId
			""";

	private static final String DELETE_BY_USER_AND_ENDPOINT = """
			DELETE FROM push_subscription WHERE user_id = :userId AND endpoint = :endpoint
			""";

	private static final String DELETE_BY_ENDPOINT = "DELETE FROM push_subscription WHERE endpoint = :endpoint";

	private final JdbcClient jdbcClient;

	@Override
	public void upsert(PushSubscription subscription) {
		jdbcClient.sql(UPSERT)
				.param("userId", subscription.userId())
				.param("endpoint", subscription.endpoint())
				.param("p256dh", subscription.p256dh())
				.param("auth", subscription.auth())
				.param("userAgent", subscription.userAgent())
				.update();
	}

	@Override
	public List<PushSubscription> findByUserId(Long userId) {
		return jdbcClient.sql(FIND_BY_USER)
				.param("userId", userId)
				.query((rs, rowNum) -> new PushSubscription(
						rs.getLong("id"), rs.getLong("user_id"), rs.getString("endpoint"),
						rs.getString("p256dh"), rs.getString("auth"), rs.getString("user_agent")))
				.list();
	}

	@Override
	public void deleteByUserIdAndEndpoint(Long userId, String endpoint) {
		jdbcClient.sql(DELETE_BY_USER_AND_ENDPOINT).param("userId", userId).param("endpoint", endpoint).update();
	}

	@Override
	public void deleteByEndpoint(String endpoint) {
		jdbcClient.sql(DELETE_BY_ENDPOINT).param("endpoint", endpoint).update();
	}
}
