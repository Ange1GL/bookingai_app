package com.github.angellariosacosta.bookingapp.application.result;

import com.github.angellariosacosta.bookingapp.domain.model.NoShow;

public record RegisterNoShowResult(
		NoShow noShow,
		long activeNoShows,
		boolean customerBlacklisted
) {
}
