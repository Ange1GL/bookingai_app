package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.application.result.RateLimitDecision;

/** Límite de mensajes por teléfono para acotar el gasto con el proveedor de IA. */
public interface CustomerRateLimitPort {
	RateLimitDecision tryAcquire(String customerPhone);
}
