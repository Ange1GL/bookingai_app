package com.github.angellariosacosta.bookingapp.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.QueryAppointmentsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidAppointmentTimeRangeException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueryAppointmentsService implements QueryAppointmentsUseCase {

	// 42 días = 6 semanas: lo máximo que pinta una vista de mes (con días de relleno).
	private static final long MAX_RANGE_DAYS = 42;

	private final AppointmentRepositoryPort appointmentRepository;

	@Override
	@Transactional(readOnly = true)
	public Appointment findById(Long id, Long userId) {
		return appointmentRepository.findById(id, userId)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with id: " + id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Appointment> findByCustomer(Long customerId, Long userId) {
		return appointmentRepository.findByCustomerId(customerId, userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Appointment> findByTimeSlot(Long userId, LocalDate date, LocalTime hour) {
		LocalDateTime from = date.atTime(hour);
		LocalDateTime to = from.plusMinutes(1);
		return appointmentRepository.findByTimeSlot(userId, from, to);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Appointment> findByDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
		Appointment.validateTimeRange(from, to);
		if (Duration.between(from, to).toDays() > MAX_RANGE_DAYS) {
			throw new InvalidAppointmentTimeRangeException(
					"The requested range cannot exceed %d days".formatted(MAX_RANGE_DAYS));
		}
		return appointmentRepository.findByTimeSlot(userId, from, to);
	}
}
