package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

import org.springframework.http.HttpStatus;

public record ErrorResponse(String timestamp, int status, String error, String message) {

	public static ErrorResponse of(HttpStatus status, String message) {
		return new ErrorResponse(
				Instant.now().toString(),
				status.value(),
				status.getReasonPhrase(),
				message
		);
	}
}
