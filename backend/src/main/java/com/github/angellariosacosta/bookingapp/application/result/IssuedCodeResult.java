package com.github.angellariosacosta.bookingapp.application.result;

import com.github.angellariosacosta.bookingapp.domain.model.User;

/**
 * Usuario al que se le emitió un código de un solo uso. {@code delivered} indica si el mensaje de WhatsApp salió.
 * El código va en claro solo para que la capa REST pueda mostrarlo en desarrollo; nunca se loguea.
 */
public record IssuedCodeResult(User user, boolean delivered, String code) {

	@Override
	public String toString() {
		return "IssuedCodeResult[userId=" + user.getId() + ", delivered=" + delivered + "]";
	}
}
