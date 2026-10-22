package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.command.ChatWithAssistantCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.ChatWithAssistantUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.AssistantChatPort;
import com.github.angellariosacosta.bookingapp.application.result.AssistantReply;
import com.github.angellariosacosta.bookingapp.application.result.ChatWithAssistantResult;

import lombok.RequiredArgsConstructor;

/**
 * Chat del asistente con créditos: rechaza ANTES de llamar al proveedor si no hay saldo y cobra los tokens
 * reales DESPUÉS. Si el proveedor falla no se cobra (no hay uso conocido). Sin {@code @Transactional} a
 * propósito: no se mantiene una transacción abierta durante la llamada HTTP al proveedor.
 */
@Service
@RequiredArgsConstructor
public class ChatWithAssistantService implements ChatWithAssistantUseCase {

	private final AssistantChatPort assistant;
	private final AiCreditsTracker credits;

	@Override
	public ChatWithAssistantResult chat(ChatWithAssistantCommand command) {
		credits.assertAvailable(command.userId());
		AssistantReply reply = assistant.chat(command.userId(), command.sessionId(), command.message());
		credits.record(command.userId(), reply.tokensUsed());
		return new ChatWithAssistantResult(reply.text(), credits.snapshot(command.userId()));
	}
}
