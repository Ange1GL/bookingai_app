package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.TranscribeVoiceCommand;
import com.github.angellariosacosta.bookingapp.application.result.VoiceTranscriptionResult;

public interface TranscribeVoiceUseCase {
	VoiceTranscriptionResult transcribe(TranscribeVoiceCommand command);
}
