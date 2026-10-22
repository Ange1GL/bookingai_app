package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.port.in.GetVoiceQuotaUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.VoiceUsageRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.VoiceQuota;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetVoiceQuotaService implements GetVoiceQuotaUseCase {

	private final VoiceUsageRepositoryPort voiceUsage;
	private final VoiceTranscriptionPolicy policy;
	private final Clock clock;

	@Override
	public VoiceQuota getQuota(Long userId) {
		LocalDate today = BusinessDay.today(clock);
		boolean monthlyExhausted = voiceUsage.monthlyCount(today.withDayOfMonth(1)) >= policy.monthlyLimit();
		return new VoiceQuota(policy.dailyLimitPerUser(), voiceUsage.dailyCount(userId, today), monthlyExhausted,
				BusinessDay.nextStart(clock));
	}
}
