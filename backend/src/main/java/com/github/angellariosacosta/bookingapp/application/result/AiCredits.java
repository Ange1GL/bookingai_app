package com.github.angellariosacosta.bookingapp.application.result;

import java.time.OffsetDateTime;

/**
 * Créditos de AI de un usuario en el día de negocio actual (1 crédito = 1 token del proveedor).
 *
 * @param dailyLimit tokens que el usuario puede gastar por día
 * @param used       tokens gastados hoy (puede superar el límite por el último mensaje)
 * @param resetsAt   inicio del siguiente día de negocio, cuando el contador vuelve a cero
 */
public record AiCredits(long dailyLimit, long used, OffsetDateTime resetsAt) {

	private static final int PERCENT = 100;

	public long remaining() {
		return Math.max(0, dailyLimit - used);
	}

	public boolean exhausted() {
		return remaining() == 0;
	}

	public int percentUsed() {
		return (int) Math.min(PERCENT, used * PERCENT / dailyLimit);
	}
}
