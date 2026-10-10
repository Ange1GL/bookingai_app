package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.CreateAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.QueryAppointmentsUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AppointmentResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateAppointmentRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.AppointmentRestMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {



	private final CreateAppointmentUseCase createAppointment;
	private final QueryAppointmentsUseCase queryAppointments;
	private final AppointmentRestMapper mapper;

	@PostMapping
	public ResponseEntity<AppointmentResponse> create(
			@Valid @RequestBody CreateAppointmentRequest request,
			@CurrentUserId Long userId) {
		Appointment appointment = createAppointment.create(mapper.toCommand(request, userId));
		return ResponseEntity
				.ok((mapper.toResponse(appointment)));

	}

	// Una sola consulta sirve a la vista semanal y mensual del calendario: el front manda el
	// rango visible [from, to) y el backend no necesita saber qué vista se está pintando.
	@GetMapping
	public ResponseEntity<List<AppointmentResponse>> findByDateRange(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@CurrentUserId Long userId) {
		List<AppointmentResponse> appointments = queryAppointments.findByDateRange(userId, from, to).stream()
				.map(mapper::toResponse)
				.toList();
		return ResponseEntity.ok(appointments);
	}
}
