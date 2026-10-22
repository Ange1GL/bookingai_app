package com.github.angellariosacosta.bookingapp.application.port.out;

import java.time.Instant;

/**
 * Comprobante firmado de que el usuario ya demostró conocer su código de un solo uso. Solo sirve para ese usuario y
 * ese código (si se emite otro o se consume, deja de valer) y caduca solo; no se guarda en base de datos.
 */
public interface VerificationTicketPort {

	String issue(Long userId, Long codeId, Instant expiresAt);

	/** Falso si la firma no cuadra, el formato es inválido, caducó o no pertenece a ese usuario y código. */
	boolean isValid(String ticket, Long userId, Long codeId, Instant now);
}
