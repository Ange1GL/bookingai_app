package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.command.TranscribeVoiceCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.TranscribeVoiceUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.SpeechToTextPort;
import com.github.angellariosacosta.bookingapp.application.port.out.VoiceUsageRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.VoiceTranscriptionResult;
import com.github.angellariosacosta.bookingapp.domain.exception.EmptyTranscriptionException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidVoiceAudioException;
import com.github.angellariosacosta.bookingapp.domain.exception.SpeechToTextUnavailableException;
import com.github.angellariosacosta.bookingapp.domain.exception.VoiceQuotaExceededException;
import com.github.angellariosacosta.bookingapp.domain.model.VoiceAudioFormat;

import lombok.RequiredArgsConstructor;

/**
 * Dictado por voz del asistente: valida el audio, consume la cuota y lo transcribe.
 *
 * <p>Para cuidar el gasto con el proveedor: todo lo inválido se rechaza ANTES de llamarlo, la cuota
 * se consume antes de la llamada (atómica, sin carreras) y no hay reintentos. Sin {@code @Transactional}
 * a propósito: no se mantiene una transacción abierta durante la llamada HTTP al proveedor.
 */
@Service
@RequiredArgsConstructor
public class TranscribeVoiceService implements TranscribeVoiceUseCase {

	// Mismo límite que ChatRequest.message: el texto termina en la caja del chat.
	static final int MAX_TRANSCRIPTION_LENGTH = 500;

	static final String UNSUPPORTED_FORMAT_MESSAGE = "Formato de audio no soportado";
	static final String TOO_SHORT_MESSAGE = "El audio es demasiado corto";
	static final String TOO_LARGE_MESSAGE = "El audio supera el tamaño máximo permitido";
	static final String DAILY_LIMIT_MESSAGE = "Alcanzaste el límite de dictados por voz de hoy";
	static final String MONTHLY_LIMIT_MESSAGE = "El dictado por voz alcanzó su límite de este mes";
	static final String EMPTY_TRANSCRIPTION_MESSAGE = "No se entendió el audio, intenta de nuevo";

	private final SpeechToTextPort speechToText;
	private final VoiceUsageRepositoryPort voiceUsage;
	private final VoiceTranscriptionPolicy policy;
	private final Clock clock;

	@Override
	public VoiceTranscriptionResult transcribe(TranscribeVoiceCommand command) {
		VoiceAudioFormat format = validatedFormat(command);
		LocalDate today = LocalDate.now(clock);
		//Casi. LocalDate.now(clock) obtiene la fecha de hoy a partir de un Clock inyectado, y es ese Clock el que define la zona horaria.

		consumeQuota(command.userId(), today);

		String text = callProvider(command, format, today).strip();
		// El proveedor ya cobró este audio: la cuota no se devuelve aunque no haya texto.
		if (text.isEmpty()) {
			throw new EmptyTranscriptionException(EMPTY_TRANSCRIPTION_MESSAGE);
		}

		int remainingToday = Math.max(0, policy.dailyLimitPerUser() - voiceUsage.dailyCount(command.userId(), today));
		return new VoiceTranscriptionResult(truncate(text), remainingToday);
	}

	private VoiceAudioFormat validatedFormat(TranscribeVoiceCommand command) {
		byte[] audio = command.audio();
		if (audio == null || audio.length < policy.minAudioBytes()) {
			throw new InvalidVoiceAudioException(TOO_SHORT_MESSAGE);
		}
		if (audio.length > policy.maxAudioBytes()) {
			throw new InvalidVoiceAudioException(TOO_LARGE_MESSAGE);
		}
		return VoiceAudioFormat.fromContentType(command.contentType())
				.filter(format -> format.matchesSignature(audio))
				.orElseThrow(() -> new InvalidVoiceAudioException(UNSUPPORTED_FORMAT_MESSAGE));
	}

	private void consumeQuota(Long userId, LocalDate today) {
		if (!voiceUsage.tryConsumeDaily(userId, today, policy.dailyLimitPerUser())) {
			throw new VoiceQuotaExceededException(DAILY_LIMIT_MESSAGE);
		}
		if (!voiceUsage.tryConsumeMonthly(today.withDayOfMonth(1), policy.monthlyLimit())) {
			voiceUsage.refundDaily(userId, today);
			throw new VoiceQuotaExceededException(MONTHLY_LIMIT_MESSAGE);
		}
	}

	private String callProvider(TranscribeVoiceCommand command, VoiceAudioFormat format, LocalDate today) {
		try {
			return speechToText.transcribe(command.audio(), format);
		} catch (SpeechToTextUnavailableException ex) {
			// Se devuelve la cuota del usuario para que pueda reintentar. La mensual NO se devuelve:
			// un timeout puede ocurrir después de que el proveedor ya cobró, y esa cuota protege el presupuesto.
			voiceUsage.refundDaily(command.userId(), today);
			throw ex;
		}
	}

	private static String truncate(String text) {
		return text.length() <= MAX_TRANSCRIPTION_LENGTH ? text : text.substring(0, MAX_TRANSCRIPTION_LENGTH).strip();
	}
}
