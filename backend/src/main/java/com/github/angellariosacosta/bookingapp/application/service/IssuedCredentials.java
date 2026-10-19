package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.domain.model.User;

/** Usuario ya persistido con su contraseña temporal en claro, pendiente de entregar. Vive solo en memoria. */
record IssuedCredentials(User user, String temporaryPassword) {

	@Override
	public String toString() {
		return "IssuedCredentials[userId=" + user.getId() + "]";
	}
}
