package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.angellariosacosta.bookingapp.domain.exception.*;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String INVALID_PARAMETER_MESSAGE =
			"El parámetro '%s' tiene un formato inválido (tipo esperado: %s). Las fechas usan el formato yyyy-MM-ddTHH:mm:ss";
	private static final String MISSING_PARAMETER_MESSAGE = "El parámetro '%s' es obligatorio";
	private static final String MISSING_PART_MESSAGE = "La parte '%s' es obligatoria";
	private static final String UPLOAD_TOO_LARGE_MESSAGE = "El archivo supera el tamaño máximo permitido";
	private static final String SPEECH_TO_TEXT_UNAVAILABLE_MESSAGE = "El dictado no está disponible en este momento";
	private static final String UNREADABLE_BODY_MESSAGE =
			"El cuerpo de la petición es inválido: revisa el JSON y el formato de las fechas (yyyy-MM-ddTHH:mm:ss)";

	@ExceptionHandler(CustomerNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleCustomerNotFound(CustomerNotFoundException ex) {
		return errorBody(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(CustomerBlacklistedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleCustomerBlacklisted(CustomerBlacklistedException ex) {
		return errorBody(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(DuplicatePriceCatalogLabelException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleDuplicatePriceCatalogLabel(DuplicatePriceCatalogLabelException ex) {
		return errorBody(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(PriceCatalogNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handlePriceCatalogNotFound(PriceCatalogNotFoundException ex) {
		return errorBody(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidFieldException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInvalidField(InvalidFieldException ex) {
		return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
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

	@ExceptionHandler(InvalidStatusTransitionException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
		return errorBody(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(AppointmentInPastException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleAppointmentInPast(AppointmentInPastException ex) {
		return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(InvalidVoiceAudioException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInvalidVoiceAudio(InvalidVoiceAudioException ex) {
		return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(EmptyTranscriptionException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
	public ErrorResponse handleEmptyTranscription(EmptyTranscriptionException ex) {
		return errorBody(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
	}

	@ExceptionHandler(VoiceQuotaExceededException.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	public ErrorResponse handleVoiceQuotaExceeded(VoiceQuotaExceededException ex) {
		return errorBody(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
	}

	// El detalle del proveedor ya quedó en el log del adaptador; al cliente solo un mensaje genérico.
	@ExceptionHandler(SpeechToTextUnavailableException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public ErrorResponse handleSpeechToTextUnavailable(SpeechToTextUnavailableException ex) {
		return errorBody(HttpStatus.BAD_GATEWAY, SPEECH_TO_TEXT_UNAVAILABLE_MESSAGE);
	}

	// Tope de spring.servlet.multipart: se corta antes de leer el archivo completo en memoria.
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	@ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
	public ErrorResponse handleUploadTooLarge(MaxUploadSizeExceededException ex) {
		return errorBody(HttpStatus.CONTENT_TOO_LARGE, UPLOAD_TOO_LARGE_MESSAGE);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMissingPart(MissingServletRequestPartException ex) {
		return errorBody(HttpStatus.BAD_REQUEST, MISSING_PART_MESSAGE.formatted(ex.getRequestPartName()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return errorBody(HttpStatus.BAD_REQUEST, message);
	}

	// Validación de @RequestParam/@PathVariable con constraints (ej. size fuera de rango en listados).
	@ExceptionHandler(HandlerMethodValidationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMethodValidation(HandlerMethodValidationException ex) {
		String message = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream())
				.map(MessageSourceResolvable::getDefaultMessage)
				.filter(Objects::nonNull)
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
