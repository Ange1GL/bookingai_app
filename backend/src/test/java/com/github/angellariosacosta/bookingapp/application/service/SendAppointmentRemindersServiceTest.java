package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentReminderRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.AppointmentReminder;
import com.github.angellariosacosta.bookingapp.application.result.PushMessage;

@ExtendWith(MockitoExtension.class)
class SendAppointmentRemindersServiceTest {

	private static final int LEAD_MINUTES = 5;
	// 2026-10-14T22:25Z = 16:25 en Ciudad de México (UTC-6).
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T22:25:00Z"), ZoneId.of("America/Mexico_City"));
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 16, 25);
	private static final LocalDateTime START = LocalDateTime.of(2026, 10, 14, 16, 30);

	@Mock
	private AppointmentReminderRepositoryPort reminders;

	@Mock
	private PushNotifier notifier;

	private SendAppointmentRemindersService service;

	@BeforeEach
	void setUp() {
		PushPolicy policy = new PushPolicy("public-key", LEAD_MINUTES, List.of("fcm.googleapis.com"));
		service = new SendAppointmentRemindersService(reminders, notifier, policy, CLOCK);
	}

	@Test
	@DisplayName("reclama las citas de los próximos minutos y avisa al dueño de cada una")
	void claimsWindowAndNotifiesOwner() {
		when(reminders.claimDue(NOW, NOW.plusMinutes(LEAD_MINUTES)))
				.thenReturn(List.of(new AppointmentReminder(11L, 7L, "Juan Pérez", "Corte y barba", START)));

		int processed = service.sendDueReminders();

		assertEquals(1, processed);
		ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
		verify(notifier).sendToUser(eq(7L), message.capture());
		assertEquals("En 5 min: Juan Pérez", message.getValue().title());
		assertEquals("/appointments", message.getValue().url());
		assertEquals("appointment-11", message.getValue().tag());
		assertEquals(true, message.getValue().body().startsWith("Corte y barba · 4:30"));
	}

	@Test
	@DisplayName("sin citas por empezar no envía nada")
	void nothingDue() {
		when(reminders.claimDue(any(), any())).thenReturn(List.of());

		assertEquals(0, service.sendDueReminders());

		verifyNoInteractions(notifier);
	}

	@Test
	@DisplayName("con el push sin configurar no reclama ninguna cita")
	void notConfiguredDoesNothing() {
		PushPolicy disabled = new PushPolicy("", LEAD_MINUTES, List.of("fcm.googleapis.com"));
		service = new SendAppointmentRemindersService(reminders, notifier, disabled, CLOCK);

		assertEquals(0, service.sendDueReminders());

		verifyNoInteractions(reminders, notifier);
	}

	@Test
	@DisplayName("si falla el aviso de una cita sigue con las demás")
	void oneFailureDoesNotStopTheOthers() {
		AppointmentReminder first = new AppointmentReminder(1L, 7L, "Ana", null, START);
		AppointmentReminder second = new AppointmentReminder(2L, 8L, "Luis", "Corte", START);
		when(reminders.claimDue(any(), any())).thenReturn(List.of(first, second));
		doThrow(new IllegalStateException("db down")).when(notifier).sendToUser(eq(7L), any());

		assertEquals(2, service.sendDueReminders());

		verify(notifier).sendToUser(eq(8L), any());
	}

	@Test
	@DisplayName("redondea hacia arriba los minutos y nunca dice menos de 1")
	void minutesAreRoundedUpWithFloorOfOne() {
		AppointmentReminder reminder = new AppointmentReminder(1L, 7L, "Ana", null, NOW.plusSeconds(100));
		assertEquals("En 2 min: Ana", SendAppointmentRemindersService.toMessage(reminder, NOW).title());

		AppointmentReminder imminent = new AppointmentReminder(2L, 7L, "Ana", null, NOW.plusSeconds(10));
		assertEquals("En 1 min: Ana", SendAppointmentRemindersService.toMessage(imminent, NOW).title());
	}

	@Test
	@DisplayName("sin servicio en el catálogo el cuerpo solo trae la hora")
	void bodyWithoutService() {
		PushMessage message = SendAppointmentRemindersService.toMessage(new AppointmentReminder(1L, 7L, "Ana", null, START), NOW);

		assertEquals(true, message.body().startsWith("4:30"));
		assertEquals(false, message.body().contains("·"));
	}
}
