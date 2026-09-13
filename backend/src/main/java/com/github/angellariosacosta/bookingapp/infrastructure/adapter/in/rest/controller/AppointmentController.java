package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.BookAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AppointmentResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.BookAppointmentRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateAppointmentRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.AppointmentRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.BookAppointmentRestMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

	private final CreateAppointmentUseCase createAppointmentUseCase;
	private final AppointmentRestMapper mapper;
	private final BookAppointmentUseCase bookAppointmentUseCase;
	private final BookAppointmentRestMapper bookMapper;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AppointmentResponse create(@RequestBody CreateAppointmentRequest request, @CurrentUserId Long userId) {
		Appointment appointment = createAppointmentUseCase.create(mapper.toCommand(request, userId));
		return mapper.toResponse(appointment);
	}

	@PostMapping("/book")
	@ResponseStatus(HttpStatus.CREATED)
	public AppointmentResponse book(@RequestBody BookAppointmentRequest request, @CurrentUserId Long userId) {
		Appointment appointment = bookAppointmentUseCase.book(bookMapper.toCommand(request, userId));
		return bookMapper.toResponse(appointment);
	}
}
