package com.github.angellariosacosta.bookingapp.application.result;

import java.time.Instant;

/** Comprobante que el paso 2 debe enviar junto con la contraseña nueva; caduca en {@code expiresAt}. */
public record VerifiedCodeResult(String verificationToken, Instant expiresAt) {

	@Override
	public String toString() {
		return "VerifiedCodeResult[expiresAt=" + expiresAt + "]";
	}
}
