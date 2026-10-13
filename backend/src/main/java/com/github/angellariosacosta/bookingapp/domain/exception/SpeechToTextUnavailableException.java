package com.github.angellariosacosta.bookingapp.domain.exception;

/** El proveedor de transcripción falló o no respondió a tiempo. */
public class SpeechToTextUnavailableException extends RuntimeException {

	public SpeechToTextUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
