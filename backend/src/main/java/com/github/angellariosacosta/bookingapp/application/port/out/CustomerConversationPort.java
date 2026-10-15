package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.Optional;

import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;

/** Estado de servidor de cada conversación de WhatsApp; el modelo de IA nunca lo escribe directamente. */
public interface CustomerConversationPort {
	Optional<CompanySelection> findSelection(String conversationKey);

	void saveSelection(String conversationKey, CompanySelection selection);
}
