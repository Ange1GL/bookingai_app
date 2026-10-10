package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.angellariosacosta.bookingapp.application.command.RegisterNoShowCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.NoShowRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.RegisterNoShowResult;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.NoShowAlreadyRegisteredException;
import com.github.angellariosacosta.bookingapp.domain.exception.NoShowNotAllowedException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.BlacklistPolicy;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;
import com.github.angellariosacosta.bookingapp.domain.model.NoShow;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;

class RegisterNoShowServiceTest {

	private static final Long USER_ID = 7L;
	private static final Long CUSTOMER_ID = 3L;
	private static final Long APPOINTMENT_ID = 100L;
	private static final int THRESHOLD = 3;
	private static final String REASON = "no llego";
	private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");
	private static final ZoneId ZONE = ZoneOffset.UTC;

	private final AppointmentRepositoryPort appointmentRepository = mock(AppointmentRepositoryPort.class);
	private final NoShowRepositoryPort noShowRepository = mock(NoShowRepositoryPort.class);
	private final CustomerBlacklistRepositoryPort blacklistRepository = mock(CustomerBlacklistRepositoryPort.class);
	private final Clock clock = Clock.fixed(NOW, ZONE);
	private RegisterNoShowService service;

	@BeforeEach
	void setUp() {
		BlacklistCustomerAction action = new BlacklistCustomerAction(blacklistRepository, appointmentRepository, clock);
		service = new RegisterNoShowService(
				appointmentRepository, noShowRepository, blacklistRepository, action, new BlacklistPolicy(THRESHOLD), clock);
		when(noShowRepository.save(any())).thenAnswer(invocation -> {
			NoShow noShow = invocation.getArgument(0);
			return NoShow.reconstitute(1L, noShow.getCustomerId(), noShow.getAppointmentId(), noShow.getUserId(), noShow.getReason(), noShow.getCreatedAt());
		});
		when(blacklistRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void registersNoShowBelowThresholdWithoutBlacklisting() {
		givenAppointment(StatusAppointment.RESERVED, minutesFromNow(-60));
		when(noShowRepository.existsByAppointmentId(APPOINTMENT_ID, USER_ID)).thenReturn(false);
		when(noShowRepository.countActiveByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(2L);
		when(blacklistRepository.existsByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(false);

		RegisterNoShowResult result = service.register(new RegisterNoShowCommand(APPOINTMENT_ID, USER_ID, REASON));

		assertEquals(2, result.activeNoShows());
		assertEquals(REASON, result.noShow().getReason());
		assertFalse(result.customerBlacklisted());
		verify(blacklistRepository, never()).save(any());
	}

	@Test
	void reachingThresholdBlacklistsAutomaticallyAndCancelsFutureAppointments() {
		givenAppointment(StatusAppointment.RESERVED, minutesFromNow(-60));
		when(noShowRepository.countActiveByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn((long) THRESHOLD);
		when(blacklistRepository.existsByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(false);
		when(appointmentRepository.cancelReservedFrom(anyLong(), anyLong(), any())).thenReturn(2);

		RegisterNoShowResult result = service.register(new RegisterNoShowCommand(APPOINTMENT_ID, USER_ID, REASON));

		assertTrue(result.customerBlacklisted());
		ArgumentCaptor<CustomerBlacklist> entry = ArgumentCaptor.forClass(CustomerBlacklist.class);
		verify(blacklistRepository).save(entry.capture());
		assertEquals("Auto: 3 inasistencias", entry.getValue().getReason());
		assertEquals(CUSTOMER_ID, entry.getValue().getCustomerId());
		verify(appointmentRepository).cancelReservedFrom(CUSTOMER_ID, USER_ID, LocalDateTime.ofInstant(NOW, ZONE));
	}

	@Test
	void doesNotBlacklistAgainWhenAlreadyBlacklisted() {
		givenAppointment(StatusAppointment.RESERVED, minutesFromNow(-60));
		when(noShowRepository.countActiveByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(5L);
		when(blacklistRepository.existsByCustomerId(CUSTOMER_ID, USER_ID)).thenReturn(true);

		RegisterNoShowResult result = service.register(new RegisterNoShowCommand(APPOINTMENT_ID, USER_ID, REASON));

		assertTrue(result.customerBlacklisted());
		verify(blacklistRepository, never()).save(any());
		verify(appointmentRepository, never()).cancelReservedFrom(anyLong(), anyLong(), any());
	}

	@Test
	void rejectsAppointmentThatDoesNotExistInTenant() {
		when(appointmentRepository.findById(APPOINTMENT_ID, USER_ID)).thenReturn(Optional.empty());

		assertThrows(AppointmentNotFoundException.class,
				() -> service.register(new RegisterNoShowCommand(APPOINTMENT_ID, USER_ID, REASON)));
		verify(noShowRepository, never()).save(any());
	}

	@Test
	void rejectsAppointmentThatHasNotStarted() {
		givenAppointment(StatusAppointment.RESERVED, minutesFromNow(60));

		assertThrows(NoShowNotAllowedException.class,
				() -> service.register(new RegisterNoShowCommand(APPOINTMENT_ID, USER_ID, REASON)));
		verify(noShowRepository, never()).save(any());
	}

	@Test
	void rejectsCancelledAppointment() {
		givenAppointment(StatusAppointment.CANCELLED, minutesFromNow(-60));

		assertThrows(NoShowNotAllowedException.class,
				() -> service.register(new RegisterNoShowCommand(APPOINTMENT_ID, USER_ID, REASON)));
	}

	@Test
	void rejectsDuplicateNoShowForSameAppointment() {
		givenAppointment(StatusAppointment.RESERVED, minutesFromNow(-60));
		when(noShowRepository.existsByAppointmentId(APPOINTMENT_ID, USER_ID)).thenReturn(true);

		assertThrows(NoShowAlreadyRegisteredException.class,
				() -> service.register(new RegisterNoShowCommand(APPOINTMENT_ID, USER_ID, REASON)));
		verify(noShowRepository, never()).save(any());
	}

	private LocalDateTime minutesFromNow(long minutes) {
		return LocalDateTime.ofInstant(NOW, ZONE).plusMinutes(minutes);
	}

	private void givenAppointment(StatusAppointment status, LocalDateTime start) {
		Customer customer = Customer.builder().id(CUSTOMER_ID).userId(USER_ID).build();
		PriceCatalog serviceItem = new PriceCatalog(1, 100, "Corte", USER_ID, true);
		Appointment appointment = Appointment.reconstitute(
				APPOINTMENT_ID, start, start.plusMinutes(30), customer, status, USER_ID, serviceItem);
		when(appointmentRepository.findById(APPOINTMENT_ID, USER_ID)).thenReturn(Optional.of(appointment));
	}
}
