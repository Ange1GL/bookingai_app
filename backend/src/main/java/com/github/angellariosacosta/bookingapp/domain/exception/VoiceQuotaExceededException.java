package com.github.angellariosacosta.bookingapp.domain.exception;

/** Se agotó la cuota de dictados por voz (diaria del usuario o mensual global). */
public class VoiceQuotaExceededException extends RuntimeException {

	public VoiceQuotaExceededException(String message) {
		super(message);
	}
}
