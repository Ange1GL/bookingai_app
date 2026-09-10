package com.github.angellariosacosta.bookingapp.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.QueryAppointmentsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueryAppointmentsService implements QueryAppointmentsUseCase {

	private final AppointmentRepository appointmentRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Appointment> findByCustomer(Long customerId) {
		return appointmentRepository.findByCustomerId(customerId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Appointment> findByTimeSlot(LocalDate date, LocalTime hour) {
		LocalDateTime from = date.atTime(hour);
		LocalDateTime to = from.plusMinutes(1);
		return appointmentRepository.findByTimeSlot(from, to);
	}
}
