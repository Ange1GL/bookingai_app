package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.ChatWithAssistantCommand;
import com.github.angellariosacosta.bookingapp.application.result.AiCredits;
import com.github.angellariosacosta.bookingapp.application.result.ChatWithAssistantResult;
import com.github.angellariosacosta.bookingapp.application.result.VoiceQuota;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AssistantChatResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreditsResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.VoiceQuotaResponse;

@Component
public class AssistantRestMapper {

	public ChatWithAssistantCommand toCommand(ChatRequest request, Long userId) {
		return new ChatWithAssistantCommand(userId, request.sessionId(), request.message());
	}

	public AssistantChatResponse toResponse(ChatWithAssistantResult result) {
		return new AssistantChatResponse(result.reply(), toResponse(result.credits()));
	}

	public VoiceQuotaResponse toResponse(VoiceQuota quota) {
		return new VoiceQuotaResponse(quota.dailyLimit(), quota.used(), quota.remaining(), quota.percentUsed(),
				quota.monthlyExhausted(), quota.resetsAt());
	}

	public CreditsResponse toResponse(AiCredits credits) {
		return new CreditsResponse(credits.dailyLimit(), credits.used(), credits.remaining(),
				credits.percentUsed(), credits.resetsAt());
	}
}
