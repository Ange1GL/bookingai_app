package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.command.TranscribeVoiceCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.SpeechToTextPort;
import com.github.angellariosacosta.bookingapp.application.port.out.VoiceUsageRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.VoiceTranscriptionResult;
import com.github.angellariosacosta.bookingapp.domain.exception.EmptyTranscriptionException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidVoiceAudioException;
import com.github.angellariosacosta.bookingapp.domain.exception.SpeechToTextUnavailableException;
import com.github.angellariosacosta.bookingapp.domain.exception.VoiceQuotaExceededException;
import com.github.angellariosacosta.bookingapp.domain.model.VoiceAudioFormat;

@ExtendWith(MockitoExtension.class)
class TranscribeVoiceServiceTest {

	private static final Long USER_ID = 7L;
	private static final String WEBM = "audio/webm;codecs=opus";
	private static final int VALID_SIZE = 32;
	private static final int MIN_BYTES = 16;
	private static final int MAX_BYTES = 64;
	private static final int DAILY_LIMIT = 40;
	private static final int MONTHLY_LIMIT = 1500;
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Mexico_City");
	// 2026-10-14T03:00Z todavía es 13 de octubre en Ciudad de México (UTC-6).
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), BUSINESS_ZONE);
	private static final LocalDate BUSINESS_TODAY = LocalDate.of(2026, 10, 13);
	private static final LocalDate FIRST_OF_MONTH = LocalDate.of(2026, 10, 1);

	@Mock
	private SpeechToTextPort speechToText;

	@Mock
	private VoiceUsageRepositoryPort voiceUsage;

	private TranscribeVoiceService service;

	@BeforeEach
	void setUp() {
		VoiceTranscriptionPolicy policy = new VoiceTranscriptionPolicy(MIN_BYTES, MAX_BYTES, DAILY_LIMIT, MONTHLY_LIMIT);
		service = new TranscribeVoiceService(speechToText, voiceUsage, policy, CLOCK);
	}

	private static byte[] webmAudio(int size) {
		byte[] audio = new byte[size];
		audio[0] = 0x1A;
		audio[1] = 0x45;
		audio[2] = (byte) 0xDF;
		audio[3] = (byte) 0xA3;
		return audio;
	}

	private static TranscribeVoiceCommand command(byte[] audio, String contentType) {
		return new TranscribeVoiceCommand(USER_ID, audio, contentType);
	}

	private static TranscribeVoiceCommand validCommand() {
		return command(webmAudio(VALID_SIZE), WEBM);
	}

	private void allowQuota() {
		when(voiceUsage.tryConsumeDaily(USER_ID, BUSINESS_TODAY, DAILY_LIMIT)).thenReturn(true);
		when(voiceUsage.tryConsumeMonthly(FIRST_OF_MONTH, MONTHLY_LIMIT)).thenReturn(true);
	}

	@DisplayName("Should transcribe and return the trimmed text with the remaining daily quota")
	@Test
	void shouldTranscribeAndReturnRemainingQuota() {
		TranscribeVoiceCommand command = validCommand();
		allowQuota();
		when(speechToText.transcribe(command.audio(), VoiceAudioFormat.WEBM))
				.thenReturn("  Agenda a Juan mañana a las cinco  ");
		when(voiceUsage.dailyCount(USER_ID, BUSINESS_TODAY)).thenReturn(3);

		VoiceTranscriptionResult result = service.transcribe(command);

		assertEquals("Agenda a Juan mañana a las cinco", result.text());
		assertEquals(DAILY_LIMIT - 3, result.remainingToday());
	}

	@DisplayName("Should use the business day, not UTC, for the daily and monthly quota")
	@Test
	void shouldUseBusinessDayForQuota() {
		allowQuota();
		when(speechToText.transcribe(any(), any())).thenReturn("hola");

		service.transcribe(validCommand());

		verify(voiceUsage).tryConsumeDaily(USER_ID, BUSINESS_TODAY, DAILY_LIMIT);
		verify(voiceUsage).tryConsumeMonthly(FIRST_OF_MONTH, MONTHLY_LIMIT);
	}

	@DisplayName("Should reject invalid audio before consuming quota or calling the provider")
	@Test
	void shouldRejectInvalidAudioBeforeSpendingAnything() {
		assertAll(
				() -> assertThrows(InvalidVoiceAudioException.class, () -> service.transcribe(command(null, WEBM))),
				() -> assertThrows(InvalidVoiceAudioException.class,
						() -> service.transcribe(command(webmAudio(MIN_BYTES - 1), WEBM))),
				() -> assertThrows(InvalidVoiceAudioException.class,
						() -> service.transcribe(command(webmAudio(MAX_BYTES + 1), WEBM))),
				() -> assertThrows(InvalidVoiceAudioException.class,
						() -> service.transcribe(command(webmAudio(VALID_SIZE), "audio/wav"))),
				// Declara webm pero los bytes no son webm.
				() -> assertThrows(InvalidVoiceAudioException.class,
						() -> service.transcribe(command(new byte[VALID_SIZE], WEBM))));

		verifyNoInteractions(voiceUsage, speechToText);
	}

	@DisplayName("Should not call the provider when the daily quota is exhausted")
	@Test
	void shouldNotCallProviderWhenDailyQuotaIsExhausted() {
		when(voiceUsage.tryConsumeDaily(USER_ID, BUSINESS_TODAY, DAILY_LIMIT)).thenReturn(false);

		VoiceQuotaExceededException ex = assertThrows(VoiceQuotaExceededException.class,
				() -> service.transcribe(validCommand()));

		assertEquals(TranscribeVoiceService.DAILY_LIMIT_MESSAGE, ex.getMessage());
		verify(voiceUsage, never()).tryConsumeMonthly(any(), anyInt());
		verifyNoInteractions(speechToText);
	}

	@DisplayName("Should refund the daily use and not call the provider when the monthly budget is exhausted")
	@Test
	void shouldRefundDailyWhenMonthlyQuotaIsExhausted() {
		when(voiceUsage.tryConsumeDaily(USER_ID, BUSINESS_TODAY, DAILY_LIMIT)).thenReturn(true);
		when(voiceUsage.tryConsumeMonthly(FIRST_OF_MONTH, MONTHLY_LIMIT)).thenReturn(false);

		VoiceQuotaExceededException ex = assertThrows(VoiceQuotaExceededException.class,
				() -> service.transcribe(validCommand()));

		assertEquals(TranscribeVoiceService.MONTHLY_LIMIT_MESSAGE, ex.getMessage());
		verify(voiceUsage).refundDaily(USER_ID, BUSINESS_TODAY);
		verifyNoInteractions(speechToText);
	}

	@DisplayName("Should refund the daily use when the provider fails")
	@Test
	void shouldRefundDailyWhenProviderFails() {
		allowQuota();
		when(speechToText.transcribe(any(), any()))
				.thenThrow(new SpeechToTextUnavailableException("down", new RuntimeException()));

		assertThrows(SpeechToTextUnavailableException.class, () -> service.transcribe(validCommand()));

		verify(voiceUsage).refundDaily(USER_ID, BUSINESS_TODAY);
	}

	@DisplayName("Should keep the quota consumed when the audio had no speech")
	@Test
	void shouldKeepQuotaWhenTranscriptionIsEmpty() {
		allowQuota();
		when(speechToText.transcribe(any(), any())).thenReturn("   ");

		assertThrows(EmptyTranscriptionException.class, () -> service.transcribe(validCommand()));

		verify(voiceUsage, never()).refundDaily(any(), any());
	}

	@DisplayName("Should truncate the text to the chat message limit")
	@Test
	void shouldTruncateToChatLimit() {
		allowQuota();
		char[] longText = new char[TranscribeVoiceService.MAX_TRANSCRIPTION_LENGTH + 50];
		Arrays.fill(longText, 'a');
		when(speechToText.transcribe(any(), any())).thenReturn(new String(longText));

		VoiceTranscriptionResult result = service.transcribe(validCommand());

		assertEquals(TranscribeVoiceService.MAX_TRANSCRIPTION_LENGTH, result.text().length());
	}
}
