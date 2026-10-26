package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.github.angellariosacosta.bookingapp.application.command.ChatWithAssistantCommand;
import com.github.angellariosacosta.bookingapp.application.command.TranscribeVoiceCommand;
import com.github.angellariosacosta.bookingapp.application.result.AiCredits;
import com.github.angellariosacosta.bookingapp.application.result.ChatWithAssistantResult;
import com.github.angellariosacosta.bookingapp.application.result.VoiceQuota;
import com.github.angellariosacosta.bookingapp.application.result.VoiceTranscriptionResult;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidVoiceAudioException;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AssistantChatResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ChatRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreditsResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.TranscriptionResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.VoiceQuotaResponse;

@Component
public class AssistantRestMapper {

	private static final String UNREADABLE_AUDIO = "No se pudo leer el audio";

	public ChatWithAssistantCommand toCommand(ChatRequest request, Long userId) {
		return new ChatWithAssistantCommand(userId, request.sessionId(), request.message());
	}

	public TranscribeVoiceCommand toCommand(MultipartFile audio, Long userId) {
		return new TranscribeVoiceCommand(userId, readBytes(audio), audio.getContentType());
	}

	public TranscriptionResponse toResponse(VoiceTranscriptionResult result) {
		return new TranscriptionResponse(result.text(), result.remainingToday());
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

	private static byte[] readBytes(MultipartFile audio) {
		try {
			return audio.getBytes();
		} catch (IOException ex) {
			throw new InvalidVoiceAudioException(UNREADABLE_AUDIO, ex);
		}
	}
}
