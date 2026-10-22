package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiCreditsPolicyTest {

	@Test
	@DisplayName("acepta un tope global igual o mayor al del usuario")
	void acceptsValidLimits() {
		assertDoesNotThrow(() -> new AiCreditsPolicy(1000, 1000));
	}

	@Test
	@DisplayName("rechaza límites no positivos o un global menor al del usuario")
	void rejectsInvalidLimits() {
		assertThrows(IllegalArgumentException.class, () -> new AiCreditsPolicy(0, 1000));
		assertThrows(IllegalArgumentException.class, () -> new AiCreditsPolicy(1000, 999));
	}
}
