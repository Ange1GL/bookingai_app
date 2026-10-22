package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.ChatWithAssistantCommand;
import com.github.angellariosacosta.bookingapp.application.result.ChatWithAssistantResult;

public interface ChatWithAssistantUseCase {

	ChatWithAssistantResult chat(ChatWithAssistantCommand command);
}
