package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.UserCredentialNotifierPort;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCredentialsResult;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Entrega la contraseña temporal por WhatsApp. Un fallo no revierte nada: se informa con {@code delivered=false}. */
@Slf4j
@Component
@RequiredArgsConstructor
class CredentialsDelivery {

	private final UserCredentialNotifierPort notifier;

	IssuedCredentialsResult deliver(IssuedCredentials issued) {
		User user = issued.user();
		boolean delivered = notifier.sendTemporaryPassword(user.getPhone(), user.getUsername(), issued.temporaryPassword());
		if (!delivered) {
			log.warn("Contraseña temporal NO entregada por WhatsApp userId={}; el admin puede reintentar con reset-password",
					user.getId());
		}
		return new IssuedCredentialsResult(user, delivered, issued.temporaryPassword());
	}
}
