package com.github.angellariosacosta.bookingapp.domain.model;

import lombok.Getter;

/** Para qué se emitió un código de un solo uso. Refleja 1:1 la tabla {@code one_time_code_purpose} (mapeo por id). */
@Getter
public enum OneTimeCodePurpose {

	// La cuenta se acaba de crear y aún no tiene contraseña.
	ACCOUNT_ACTIVATION(1),

	// Un admin invalidó la contraseña anterior y el usuario debe crear una nueva.
	PASSWORD_RESET(2);

	private final int id;

	OneTimeCodePurpose(int id) {
		this.id = id;
	}

	public static OneTimeCodePurpose fromId(int id) {
		for (OneTimeCodePurpose purpose : values()) {
			if (purpose.id == id) {
				return purpose;
			}
		}
		throw new IllegalStateException("Unknown one-time code purpose id: " + id);
	}
}
