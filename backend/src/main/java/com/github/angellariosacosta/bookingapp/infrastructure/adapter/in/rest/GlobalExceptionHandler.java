package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.angellariosacosta.bookingapp.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String INVALID_PARAMETER_MESSAGE =
			"El parámetro '%s' tiene un formato inválido (tipo esperado: %s). Las fechas usan el formato yyyy-MM-ddTHH:mm:ss";
	private static final String MISSING_PARAMETER_MESSAGE = "El parámetro '%s' es obligatorio";
	private static final String UNREADABLE_BODY_MESSAGE =
			"El cuerpo de la petición es inválido: revisa el JSON y el formato de las fechas (yyyy-MM-ddTHH:mm:ss)";

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

	@ExceptionHandler(AppointmentInPastException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleAppointmentInPast(AppointmentInPastException ex) {
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

	// Se lanza cuando un @RequestParam o @PathVariable no se puede convertir a su tipo Java,
	// por ejemplo from=hola en un parámetro LocalDateTime con formato ISO.
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String expectedType = Objects.requireNonNullElse(ex.getRequiredType(), Object.class).getSimpleName();
		return errorBody(HttpStatus.BAD_REQUEST, INVALID_PARAMETER_MESSAGE.formatted(ex.getName(), expectedType));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMissingParameter(MissingServletRequestParameterException ex) {
		return errorBody(HttpStatus.BAD_REQUEST, MISSING_PARAMETER_MESSAGE.formatted(ex.getParameterName()));
	}

	// JSON mal formado o con un valor que no se puede deserializar (ej. startTime: "").
	// No se expone el detalle de Jackson para no filtrar clases ni estructura interna.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException ex) {
		return errorBody(HttpStatus.BAD_REQUEST, UNREADABLE_BODY_MESSAGE);
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ErrorResponse> handleAuth(AuthException ex) {
		HttpStatus status = resolveAuthStatus(ex.getError());
		return ResponseEntity.status(status).body(errorBody(status, ex.getMessage()));
	}


	@ExceptionHandler(AccountBlockedException.class)
	public ResponseEntity<ErrorResponse> handleAccountBlocked(AccountBlockedException ex) {
		HttpStatus status = HttpStatus.FORBIDDEN;
		return ResponseEntity.status(status).body(errorBody(status, ex.getMessage()));
	}

	private HttpStatus resolveAuthStatus(AuthError error) {
		return switch (error) {
			case INVALID_CREDENTIALS, REFRESH_TOKEN_INVALID, REFRESH_TOKEN_EXPIRED, REFRESH_TOKEN_REUSED
					-> HttpStatus.UNAUTHORIZED;
			case USER_DISABLED        -> HttpStatus.FORBIDDEN;
			case EMAIL_ALREADY_IN_USE, USERNAME_ALREADY_IN_USE -> HttpStatus.CONFLICT;
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
