package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidStatusAppointmentExcepcion;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomerNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, Object> handleCustomerNotFound(CustomerNotFoundException ex) {
		return errorBody(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(AppointmentOverlapException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, Object> handleOverlap(AppointmentOverlapException ex) {
		return errorBody(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(InvalidStatusAppointmentExcepcion.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> handleInvalidStatus(InvalidStatusAppointmentExcepcion ex) {
		return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	private Map<String, Object> errorBody(HttpStatus status, String message) {
		return Map.of(
				"timestamp", Instant.now().toString(),
				"status", status.value(),
				"error", status.getReasonPhrase(),
				"message", message
		);
	}
}
