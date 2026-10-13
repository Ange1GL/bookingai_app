package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Límites del dictado por voz ({@code assistant.voice.*}). Ver docs/voice-transcription.md. */
@Component
@ConfigurationProperties(prefix = "assistant.voice")
@Validated
@Getter
@Setter
public class VoiceProperties {

	@Positive
	private int minAudioBytes;

	@Positive
	private int maxAudioBytes;

	@Positive
	private int dailyLimitPerUser;

	@Positive
	private int monthlyLimit;
}
