package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.domain.model.VoiceAudioFormat;

/** Proveedor de transcripción de voz a texto. Lanza {@code SpeechToTextUnavailableException} si falla. */
public interface SpeechToTextPort {
	String transcribe(byte[] audio, VoiceAudioFormat format);
}
