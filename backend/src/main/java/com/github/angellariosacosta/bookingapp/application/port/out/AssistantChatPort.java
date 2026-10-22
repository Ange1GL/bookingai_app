package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.application.result.AssistantReply;

public interface AssistantChatPort {

	/**
	 * @throws com.github.angellariosacosta.bookingapp.domain.exception.AssistantUnavailableException si el proveedor falla
	 */
	AssistantReply chat(Long userId, String sessionId, String message);
}
