package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.result.AiCredits;

public interface GetAiCreditsUseCase {

	AiCredits getCredits(Long userId);
}
