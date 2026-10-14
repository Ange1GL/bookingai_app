package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.angellariosacosta.bookingapp.application.service.VoiceTranscriptionPolicy;

@Configuration
public class VoiceConfig {

	@Bean
	VoiceTranscriptionPolicy voiceTranscriptionPolicy(VoiceProperties properties) {
		return new VoiceTranscriptionPolicy(
				properties.getMinAudioBytes(),
				properties.getMaxAudioBytes(),
				properties.getDailyLimitPerUser(),
				properties.getMonthlyLimit());
	}
}
