package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.ai;

import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AssistantChatPort;
import com.github.angellariosacosta.bookingapp.application.result.AssistantReply;
import com.github.angellariosacosta.bookingapp.domain.exception.AssistantUnavailableException;

import lombok.extern.slf4j.Slf4j;

/**
 * Agente del dueño del negocio (DeepSeek). El {@code usage} de la respuesta ya suma todas las llamadas
 * internas del tool calling (DeepSeekChatModel acumula con {@code UsageCalculator.getCumulativeUsage}).
 */
@Slf4j
@Component
public class SpringAiAssistantChatAdapter implements AssistantChatPort {

	private final ChatClient chatClient;

	public SpringAiAssistantChatAdapter(@Qualifier("chatClient") ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@Override
	public AssistantReply chat(Long userId, String sessionId, String message) {
		try {
			ChatResponse response = chatClient.prompt()
					.user(message)
					// La memoria se indexa por usuario + sesión: un sessionId ajeno nunca resuelve la conversación de otro usuario.
					.advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, userId + ":" + sessionId))
					.call()
					.chatResponse();
			long tokens = tokensUsed(response);
			log.debug("Assistant chat usage: userId={} tokens={}", userId, tokens);
			return new AssistantReply(response.getResult().getOutput().getText(), tokens);
		} catch (Exception ex) {
			log.error("Error processing chat request: {}", ex.getMessage(), ex);
			throw new AssistantUnavailableException(ex);
		}
	}

	private static long tokensUsed(ChatResponse response) {
		Usage usage = response.getMetadata().getUsage();
		return Objects.requireNonNullElse(usage.getTotalTokens(), 0);
	}
}
