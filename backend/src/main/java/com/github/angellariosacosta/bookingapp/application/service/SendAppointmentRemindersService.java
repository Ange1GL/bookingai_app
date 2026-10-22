package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.port.in.SendAppointmentRemindersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentReminderRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.AppointmentReminder;
import com.github.angellariosacosta.bookingapp.application.result.PushMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recordatorio push de cada cita {@code leadMinutes} antes de empezar. Las citas se reclaman de forma
 * atómica (ver {@link AppointmentReminderRepositoryPort}), así que no se avisa dos veces aunque haya varias
 * instancias. Si el envío falla no se reintenta: un recordatorio tardío ya no sirve. Sin
 * {@code @Transactional}: las llamadas HTTP al servicio push no deben mantener una transacción abierta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendAppointmentRemindersService implements SendAppointmentRemindersUseCase {

	static final String APPOINTMENTS_URL = "/appointments";
	private static final String TAG_PREFIX = "appointment-";
	private static final String BODY_SEPARATOR = " · ";
	private static final int SECONDS_PER_MINUTE = 60;
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-MX"));

	private final AppointmentReminderRepositoryPort reminders;
	private final PushNotifier notifier;
	private final PushPolicy policy;
	private final Clock clock;

	@Override
	public int sendDueReminders() {
		if (!policy.configured()) {
			return 0;
		}
		LocalDateTime now = LocalDateTime.now(clock);
		List<AppointmentReminder> due = reminders.claimDue(now, now.plusMinutes(policy.leadMinutes()));
		due.forEach(reminder -> notify(reminder, now));
		return due.size();
	}

	private void notify(AppointmentReminder reminder, LocalDateTime now) {
		try {
			notifier.sendToUser(reminder.userId(), toMessage(reminder, now));
		} catch (RuntimeException ex) {
			// Un usuario con problemas no debe impedir el aviso de las demás citas.
			log.error("No se pudo enviar el recordatorio: appointmentId={} userId={}", reminder.appointmentId(),
					reminder.userId(), ex);
		}
	}

	static PushMessage toMessage(AppointmentReminder reminder, LocalDateTime now) {
		long secondsLeft = Math.max(0, Duration.between(now, reminder.startTime()).toSeconds());
		long minutes = Math.max(1, Math.ceilDiv(secondsLeft, SECONDS_PER_MINUTE));
		String time = TIME_FORMAT.format(reminder.startTime());
		String body = reminder.serviceLabel() == null ? time : reminder.serviceLabel() + BODY_SEPARATOR + time;
		return new PushMessage("En " + minutes + " min: " + reminder.customerName(), body, APPOINTMENTS_URL,
				TAG_PREFIX + reminder.appointmentId());
	}
}
