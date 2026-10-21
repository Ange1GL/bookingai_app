package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.domain.model.User;

/** Usuario ya persistido con su código en claro, pendiente de entregar. Vive solo en memoria. */
record IssuedCode(User user, String code) {

	@Override
	public String toString() {
		return "IssuedCode[userId=" + user.getId() + "]";
	}
}
