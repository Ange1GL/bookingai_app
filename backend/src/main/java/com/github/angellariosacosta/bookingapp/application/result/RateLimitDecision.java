package com.github.angellariosacosta.bookingapp.application.result;

public enum RateLimitDecision {
	ALLOWED,
	// Primer mensaje por encima del límite en la ventana: se avisa una sola vez al cliente.
	REJECTED_NOTIFY,
	// Resto de mensajes por encima del límite: se descartan sin responder (evita amplificar un abuso).
	REJECTED_SILENT
}
