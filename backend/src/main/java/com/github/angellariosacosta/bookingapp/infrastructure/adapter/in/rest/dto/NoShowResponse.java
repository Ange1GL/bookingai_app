package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

public record NoShowResponse(
		Long id,
		Long appointmentId,
		Long customerId,
		String reason,
		long activeNoShows,
		boolean customerBlacklisted
) {
}
