package com.github.angellariosacosta.bookingapp.domain.exception;

public class InvalidVoiceAudioException extends RuntimeException {

	public InvalidVoiceAudioException(String message) {
		super(message);
	}

	public InvalidVoiceAudioException(String message, Throwable cause) {
		super(message, cause);
	}
}
