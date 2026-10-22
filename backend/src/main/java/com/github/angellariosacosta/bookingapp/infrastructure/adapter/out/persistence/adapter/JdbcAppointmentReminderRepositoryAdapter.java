package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentReminderRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.AppointmentReminder;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;

import lombok.RequiredArgsConstructor;

/**
 * Reclamo atómico de recordatorios: una sola sentencia marca {@code reminder_sent_at} y devuelve las citas.
 * {@code FOR UPDATE SKIP LOCKED} hace que dos instancias de la app nunca reclamen la misma cita. Solo se
 * reclaman citas de usuarios con algún dispositivo suscrito: quien se suscribe después aún recibe el aviso.
 */
@Component
@RequiredArgsConstructor
public class JdbcAppointmentReminderRepositoryAdapter implements AppointmentReminderRepositoryPort {

	private static final String CLAIM_DUE = """
			WITH due AS (
			    UPDATE appointment SET reminder_sent_at = now()
			    WHERE id IN (
			        SELECT a.id FROM appointment a
			        WHERE a.status_id = :reservedId
			          AND a.reminder_sent_at IS NULL
			          AND a.start_time > :from AND a.start_time <= :to
			          AND EXISTS (SELECT 1 FROM push_subscription ps WHERE ps.user_id = a.user_id)
			        FOR UPDATE SKIP LOCKED
			    )
			    RETURNING id, user_id, customer_id, price_catalog_id, start_time
			)
			SELECT d.id, d.user_id, c.full_name, p.label, d.start_time
			FROM due d
			JOIN customer c ON c.customer_id = d.customer_id
			LEFT JOIN price_catalog p ON p.price_catalog_id = d.price_catalog_id
			ORDER BY d.start_time
			""";

	private final JdbcClient jdbcClient;

	@Override
	public List<AppointmentReminder> claimDue(LocalDateTime from, LocalDateTime to) {
		return jdbcClient.sql(CLAIM_DUE)
				.param("reservedId", StatusAppointment.RESERVED.getId())
				.param("from", from)
				.param("to", to)
				.query((rs, rowNum) -> new AppointmentReminder(
						rs.getLong("id"), rs.getLong("user_id"), rs.getString("full_name"), rs.getString("label"),
						rs.getObject("start_time", LocalDateTime.class)))
				.list();
	}
}
