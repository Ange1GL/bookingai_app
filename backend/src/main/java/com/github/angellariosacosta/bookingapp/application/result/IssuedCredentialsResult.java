package com.github.angellariosacosta.bookingapp.application.result;

import com.github.angellariosacosta.bookingapp.domain.model.User;

/**
 * Usuario con contraseña temporal recién emitida. {@code delivered} indica si el mensaje de WhatsApp salió.
 * La contraseña va en claro solo para que la capa REST pueda mostrarla en desarrollo; nunca se loguea.
 */
public record IssuedCredentialsResult(User user, boolean delivered, String temporaryPassword) {

	@Override
	public String toString() {
		return "IssuedCredentialsResult[userId=" + user.getId() + ", delivered=" + delivered + "]";
	}
}
