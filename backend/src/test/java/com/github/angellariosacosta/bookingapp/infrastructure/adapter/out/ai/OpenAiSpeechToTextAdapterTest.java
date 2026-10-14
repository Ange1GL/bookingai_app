package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;

import com.github.angellariosacosta.bookingapp.domain.exception.SpeechToTextUnavailableException;
import com.github.angellariosacosta.bookingapp.domain.model.VoiceAudioFormat;

@ExtendWith(MockitoExtension.class)
class OpenAiSpeechToTextAdapterTest {

	private static final byte[] AUDIO = {1, 2, 3};

	@Mock
	private TranscriptionModel transcriptionModel;

	@InjectMocks
	private OpenAiSpeechToTextAdapter adapter;

	@DisplayName("Should send the audio with a filename whose extension matches the format")
	@Test
	void shouldSendAudioWithFormatExtension() throws Exception {
		ArgumentCaptor<AudioTranscriptionPrompt> promptCaptor = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
		when(transcriptionModel.call(promptCaptor.capture()))
				.thenReturn(new AudioTranscriptionResponse(new AudioTranscription("hola")));

		String text = adapter.transcribe(AUDIO, VoiceAudioFormat.MP4);

		assertEquals("hola", text);
		assertEquals("voice.mp4", promptCaptor.getValue().getInstructions().getFilename());
		assertArrayEquals(AUDIO, promptCaptor.getValue().getInstructions().getContentAsByteArray());
	}

	@DisplayName("Should return an empty text when the provider returns no output")
	@Test
	void shouldReturnEmptyWhenNoOutput() {
		when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
				.thenReturn(new AudioTranscriptionResponse(new AudioTranscription(null)));

		assertEquals("", adapter.transcribe(AUDIO, VoiceAudioFormat.WEBM));
	}

	@DisplayName("Should translate provider errors into SpeechToTextUnavailableException")
	@Test
	void shouldTranslateProviderErrors() {
		RuntimeException providerError = new IllegalStateException("timeout");
		when(transcriptionModel.call(any(AudioTranscriptionPrompt.class))).thenThrow(providerError);

		SpeechToTextUnavailableException ex = assertThrows(SpeechToTextUnavailableException.class,
				() -> adapter.transcribe(AUDIO, VoiceAudioFormat.WEBM));

		assertSame(providerError, ex.getCause());
	}
}
