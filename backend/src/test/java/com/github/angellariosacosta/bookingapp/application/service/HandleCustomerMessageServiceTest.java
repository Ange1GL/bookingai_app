package com.github.angellariosacosta.bookingapp.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerAssistantPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRateLimitPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerReplyPort;
import com.github.angellariosacosta.bookingapp.application.port.out.ProcessedMessagePort;
import com.github.angellariosacosta.bookingapp.application.result.RateLimitDecision;

@ExtendWith(MockitoExtension.class)
class HandleCustomerMessageServiceTest {

	private static final String PHONE_NUMBER_ID = "1001";
	private static final String FROM = "5215512345678";
	private static final String MESSAGE_ID = "wamid.1";

	@Mock
	private CustomerAssistantPort assistant;

	@Mock
	private CustomerReplyPort replyPort;

	@Mock
	private ProcessedMessagePort processedMessages;

	@Mock
	private CustomerRateLimitPort rateLimit;

	@InjectMocks
	private HandleCustomerMessageService service;

	@BeforeEach
	void setUp() {
		lenient().when(processedMessages.markIfNew(MESSAGE_ID)).thenReturn(true);
		lenient().when(rateLimit.tryAcquire(FROM)).thenReturn(RateLimitDecision.ALLOWED);
	}

	private static HandleCustomerMessageCommand text(String body) {
		return new HandleCustomerMessageCommand(PHONE_NUMBER_ID, FROM, MESSAGE_ID, "text", body);
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
		service.handle(new HandleCustomerMessageCommand(PHONE_NUMBER_ID, FROM, MESSAGE_ID, "image", null));

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
		service.handle(new HandleCustomerMessageCommand(PHONE_NUMBER_ID, null, MESSAGE_ID, "text", "hola"));

		verifyNoInteractions(assistant, replyPort, processedMessages, rateLimit);
	}

	@DisplayName("Should ignore a message already processed (Meta retry) without calling the assistant or replying")
	@Test
	void shouldIgnoreDuplicateMessage() {
		when(processedMessages.markIfNew(MESSAGE_ID)).thenReturn(false);

		service.handle(text("hola"));

		verifyNoInteractions(assistant, replyPort, rateLimit);
	}

	@DisplayName("Should process messages without id since idempotency cannot be checked")
	@Test
	void shouldProcessMessageWithoutId() {
		HandleCustomerMessageCommand command = new HandleCustomerMessageCommand(PHONE_NUMBER_ID, FROM, null, "text", "hola");
		when(assistant.reply(command)).thenReturn("ok");

		service.handle(command);

		verifyNoInteractions(processedMessages);
		verify(replyPort).send(PHONE_NUMBER_ID, FROM, "ok");
	}

	@DisplayName("Should warn the customer once when the rate limit is exceeded and not call the assistant")
	@Test
	void shouldNotifyOnceWhenRateLimited() {
		when(rateLimit.tryAcquire(FROM)).thenReturn(RateLimitDecision.REJECTED_NOTIFY);

		service.handle(text("hola"));

		verifyNoInteractions(assistant);
		verify(replyPort).send(PHONE_NUMBER_ID, FROM, HandleCustomerMessageService.RATE_LIMITED_REPLY);
	}

	@DisplayName("Should drop silently the messages beyond the rate limit")
	@Test
	void shouldDropSilentlyWhenRateLimited() {
		when(rateLimit.tryAcquire(FROM)).thenReturn(RateLimitDecision.REJECTED_SILENT);

		service.handle(text("hola"));

		verifyNoInteractions(assistant);
		verify(replyPort, never()).send(any(), any(), any());
	}
}
