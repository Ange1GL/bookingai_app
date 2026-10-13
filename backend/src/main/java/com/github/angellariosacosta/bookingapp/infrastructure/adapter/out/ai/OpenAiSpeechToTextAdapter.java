package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.ai;

import java.util.Optional;

import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.SpeechToTextPort;
import com.github.angellariosacosta.bookingapp.domain.exception.SpeechToTextUnavailableException;
import com.github.angellariosacosta.bookingapp.domain.model.VoiceAudioFormat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Transcribe con el {@link TranscriptionModel} que autoconfigura Spring AI (OpenAI
 * {@code gpt-4o-mini-transcribe}; modelo, idioma, prompt, timeout y reintentos en
 * {@code spring.ai.openai.audio.transcription.*}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiSpeechToTextAdapter implements SpeechToTextPort {

	private static final String FILE_BASE_NAME = "voice.";
	private static final String PROVIDER_ERROR_MESSAGE = "El proveedor de transcripción no respondió";

	private final TranscriptionModel transcriptionModel;

	@Override
	public String transcribe(byte[] audio, VoiceAudioFormat format) {
		// OpenAI detecta el formato por la extensión del nombre de archivo: sin nombre rechaza el audio.
		ByteArrayResource resource = new NamedAudioResource(audio, FILE_BASE_NAME + format.extension());
		try {
			AudioTranscriptionResponse response = transcriptionModel.call(new AudioTranscriptionPrompt(resource));
			return Optional.ofNullable(response)
					.map(AudioTranscriptionResponse::getResult)
					.map(AudioTranscription::getOutput)
					.orElse("");
		} catch (RuntimeException ex) {
			// Sin datos del usuario ni del audio en el log: solo el tipo de error del proveedor.
			log.warn("Speech-to-text provider failed: {}", ex.getClass().getSimpleName());
			throw new SpeechToTextUnavailableException(PROVIDER_ERROR_MESSAGE, ex);
		}
	}

	private static final class NamedAudioResource extends ByteArrayResource {

		private final String filename;

		private NamedAudioResource(byte[] audio, String filename) {
			super(audio);
			this.filename = filename;
		}

		@Override
		public String getFilename() {
			return filename;
		}
	}
}
