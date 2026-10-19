package com.github.angellariosacosta.bookingapp.domain.exception;

/** El código no existe, venció, se agotó o no coincide. Un solo mensaje para no revelar cuál fue el motivo. */
public class InvalidOneTimeCodeException extends RuntimeException {

	public static final String MESSAGE = "Código inválido o vencido";

	public InvalidOneTimeCodeException() {
		super(MESSAGE);
	}
}
