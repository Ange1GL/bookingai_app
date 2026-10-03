package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.CreateAppointmentUseCase;
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
	private final AppointmentRestMapper mapper;

	@PostMapping
	public ResponseEntity<AppointmentResponse> create(
			@Valid @RequestBody CreateAppointmentRequest request,
			@CurrentUserId Long userId) {
		Appointment appointment = createAppointment.create(mapper.toCommand(request, userId));
		return ResponseEntity
				.ok((mapper.toResponse(appointment)));

	}
}
