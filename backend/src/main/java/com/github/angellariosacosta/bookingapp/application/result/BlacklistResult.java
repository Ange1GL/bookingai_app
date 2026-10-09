package com.github.angellariosacosta.bookingapp.application.result;

import com.github.angellariosacosta.bookingapp.domain.model.BlacklistSource;

public record BlacklistResult(
		Long customerId,
		String reason,
		BlacklistSource source,
		int cancelledAppointments
) {
}
