package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentAccessDeniedException;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentOverlapException;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidAppointmentTimeRangeException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidStatusAppointmentExcepcion;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomerNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleCustomerNotFound(CustomerNotFoundException ex) {
		return errorBody(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(AppointmentNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleAppointmentNotFound(AppointmentNotFoundException ex) {
		return errorBody(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(AppointmentAccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ErrorResponse handleAppointmentAccessDenied(AppointmentAccessDeniedException ex) {
		return errorBody(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(AppointmentOverlapException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleOverlap(AppointmentOverlapException ex) {
		return errorBody(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(InvalidStatusAppointmentExcepcion.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInvalidStatus(InvalidStatusAppointmentExcepcion ex) {
		return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(InvalidAppointmentTimeRangeException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInvalidTimeRange(InvalidAppointmentTimeRangeException ex) {
		return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return errorBody(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ErrorResponse> handleAuth(AuthException ex) {
		HttpStatus status = resolveAuthStatus(ex.getError());
		return ResponseEntity.status(status).body(errorBody(status, ex.getMessage()));
	}

	private HttpStatus resolveAuthStatus(AuthError error) {
		return switch (error) {
			case INVALID_CREDENTIALS  -> HttpStatus.UNAUTHORIZED;
			case USER_DISABLED        -> HttpStatus.FORBIDDEN;
			case EMAIL_ALREADY_IN_USE -> HttpStatus.CONFLICT;
		};
	}

	private ErrorResponse errorBody(HttpStatus status, String message) {
		return new ErrorResponse(
				Instant.now().toString(),
				status.value(),
				status.getReasonPhrase(),
				message
		);
	}
}
