package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.OneTimeCodeNotifierPort;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Entrega el código por WhatsApp. Un fallo no revierte nada: se informa con {@code delivered=false}. */
@Slf4j
@Component
@RequiredArgsConstructor
class OneTimeCodeDelivery {

	private final OneTimeCodeNotifierPort notifier;

	IssuedCodeResult deliver(IssuedCode issued) {
		User user = issued.user();
		boolean delivered = notifier.sendCode(user.getPhone(), issued.code());
		if (!delivered) {
			log.warn("Código NO entregado por WhatsApp userId={}; el admin puede reintentar con reset-password", user.getId());
		}
		return new IssuedCodeResult(user, delivered, issued.code());
	}
}
