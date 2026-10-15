package com.github.angellariosacosta.bookingapp.application.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerAssistantPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerReplyPort;

@ExtendWith(MockitoExtension.class)
class HandleCustomerMessageServiceTest {

	private static final String PHONE_NUMBER_ID = "1001";
	private static final String FROM = "5215512345678";

	@Mock
	private CustomerAssistantPort assistant;

	@Mock
	private CustomerReplyPort replyPort;

	@InjectMocks
	private HandleCustomerMessageService service;

	private static HandleCustomerMessageCommand text(String body) {
		return new HandleCustomerMessageCommand(PHONE_NUMBER_ID, FROM, "text", body);
	}

	@DisplayName("Should send the assistant reply to the customer")
	@Test
	void shouldSendAssistantReply() {
		HandleCustomerMessageCommand command = text("hola");
		when(assistant.reply(command)).thenReturn("Bienvenido");

		service.handle(command);

		verify(replyPort).send(PHONE_NUMBER_ID, FROM, "Bienvenido");
	}

	@DisplayName("Should answer with a fallback and not call the assistant for non-text messages")
	@Test
	void shouldReplyUnsupportedForNonText() {
		service.handle(new HandleCustomerMessageCommand(PHONE_NUMBER_ID, FROM, "image", null));

		verifyNoInteractions(assistant);
		verify(replyPort).send(PHONE_NUMBER_ID, FROM, HandleCustomerMessageService.UNSUPPORTED_MESSAGE_REPLY);
	}

	@DisplayName("Should answer with an apology when the assistant fails")
	@Test
	void shouldReplyApologyWhenAssistantFails() {
		HandleCustomerMessageCommand command = text("hola");
		when(assistant.reply(command)).thenThrow(new IllegalStateException("provider down"));

		service.handle(command);

		verify(replyPort).send(PHONE_NUMBER_ID, FROM, HandleCustomerMessageService.ASSISTANT_UNAVAILABLE_REPLY);
	}

	@DisplayName("Should ignore messages without sender")
	@Test
	void shouldIgnoreMessageWithoutSender() {
		service.handle(new HandleCustomerMessageCommand(PHONE_NUMBER_ID, null, "text", "hola"));

		verifyNoInteractions(assistant);
		verify(replyPort, never()).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}
}
