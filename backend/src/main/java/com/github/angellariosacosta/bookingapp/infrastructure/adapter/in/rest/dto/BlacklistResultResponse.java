package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import com.github.angellariosacosta.bookingapp.domain.model.BlacklistSource;

public record BlacklistResultResponse(
		Long customerId,
		String reason,
		BlacklistSource source,
		int cancelledAppointments
) {
}
