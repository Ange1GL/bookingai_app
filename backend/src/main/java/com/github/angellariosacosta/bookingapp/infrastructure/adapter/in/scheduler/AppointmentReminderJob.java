package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.in.SendAppointmentRemindersUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Revisa cada {@code app.push.job-interval-seconds} qué citas están por empezar. Ver docs/push-reminders.md. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderJob {

	private static final int MILLIS_PER_SECOND = 1000;

	private final SendAppointmentRemindersUseCase sendAppointmentReminders;

	@Scheduled(fixedDelayString = "#{${app.push.job-interval-seconds} * " + MILLIS_PER_SECOND + "}")
	public void sendDueReminders() {
		try {
			int processed = sendAppointmentReminders.sendDueReminders();
			if (processed > 0) {
				log.info("Recordatorios de citas procesados: {}", processed);
			}
		} catch (RuntimeException ex) {
			log.error("Falló la revisión de recordatorios de citas", ex);
		}
	}
}
