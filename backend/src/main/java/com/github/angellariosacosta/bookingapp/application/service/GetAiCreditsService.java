package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.port.in.GetAiCreditsUseCase;
import com.github.angellariosacosta.bookingapp.application.result.AiCredits;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAiCreditsService implements GetAiCreditsUseCase {

	private final AiCreditsTracker credits;

	@Override
	public AiCredits getCredits(Long userId) {
		return credits.snapshot(userId);
	}
}
