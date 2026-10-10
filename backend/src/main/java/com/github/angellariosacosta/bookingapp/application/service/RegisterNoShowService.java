package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.RegisterNoShowCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.RegisterNoShowUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.NoShowRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.RegisterNoShowResult;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.NoShowAlreadyRegisteredException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.BlacklistPolicy;
import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;
import com.github.angellariosacosta.bookingapp.domain.model.NoShow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterNoShowService implements RegisterNoShowUseCase {

	private final AppointmentRepositoryPort appointmentRepository;
	private final NoShowRepositoryPort noShowRepository;
	private final CustomerBlacklistRepositoryPort blacklistRepository;
	private final BlacklistCustomerAction blacklistAction;
	private final BlacklistPolicy blacklistPolicy;
	private final Clock clock;

	@Override
	@Transactional
	public RegisterNoShowResult register(RegisterNoShowCommand command) {
		Appointment appointment = appointmentRepository.findById(command.appointmentId(), command.userId())
				.orElseThrow(() -> new AppointmentNotFoundException(
						"Appointment not found with id: " + command.appointmentId()));
		appointment.ensureNoShowRegistrable(LocalDateTime.now(clock));

		if (noShowRepository.existsByAppointmentId(command.appointmentId(), command.userId())) {
			throw new NoShowAlreadyRegisteredException(
					"Appointment %d already has a no-show registered".formatted(command.appointmentId()));
		}

		Long customerId = appointment.getCustomer().getId();
		NoShow saved = noShowRepository.save(
				NoShow.register(customerId, command.appointmentId(), command.userId(), command.reason(), clock.instant()));
		long activeNoShows = noShowRepository.countActiveByCustomerId(customerId, command.userId());

		boolean blacklisted = blacklistRepository.existsByCustomerId(customerId, command.userId());
		if (!blacklisted && blacklistPolicy.isReached(activeNoShows)) {
			blacklistAction.apply(
					CustomerBlacklist.forNoShows(customerId, command.userId(), activeNoShows, clock.instant()));
			blacklisted = true;
		}
		return new RegisterNoShowResult(saved, activeNoShows, blacklisted);
	}
}
