package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record BookAppointmentRequest(
		String name,
		String phone,
		LocalDateTime startTime,
		LocalDateTime endTime,
		@NotNull(message = "El servicio del catálogo es obligatorio")
		Integer priceCatalogId
) {}
