package com.github.angellariosacosta.bookingapp.application.port.out;

/** Idempotencia: Meta reintenta la entrega si no recibe 200 a tiempo, y el mismo mensaje no debe procesarse dos veces. */
public interface ProcessedMessagePort {
	/** Registra el id de mensaje de forma atómica; devuelve false si ya se había registrado. */
	boolean markIfNew(String messageId);
}
