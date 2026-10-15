package com.github.angellariosacosta.bookingapp.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;
import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand;
import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand.IncomingMessage;
import com.github.angellariosacosta.bookingapp.application.port.in.HandleCustomerMessageUseCase;

@ExtendWith(MockitoExtension.class)
class ReceiveWhatsAppEventServiceTest {

	@Mock
	private HandleCustomerMessageUseCase handleCustomerMessage;

	@InjectMocks
	private ReceiveWhatsAppEventService service;

	private static IncomingMessage message(String id, String text) {
		return new IncomingMessage("1001", "5215512345678", id, "text", text);
	}

	private static HandleCustomerMessageCommand command(String id, String text) {
		return new HandleCustomerMessageCommand("1001", "5215512345678", id, "text", text);
	}

	@DisplayName("Should hand each incoming message, with its id, to the customer agent")
	@Test
	void shouldDispatchEachMessage() {
		service.receive(new ReceiveWhatsAppEventCommand(List.of(message("m1", "hola"), message("m2", "quiero cita"))));

		verify(handleCustomerMessage).handle(command("m1", "hola"));
		verify(handleCustomerMessage).handle(command("m2", "quiero cita"));
	}

	@DisplayName("Should keep processing the remaining messages when one fails (e.g. executor queue full)")
	@Test
	void shouldContinueWhenOneMessageFails() {
		doThrow(new IllegalStateException("boom")).when(handleCustomerMessage).handle(command("m1", "hola"));

		service.receive(new ReceiveWhatsAppEventCommand(List.of(message("m1", "hola"), message("m2", "otra"))));

		verify(handleCustomerMessage, times(2)).handle(any(HandleCustomerMessageCommand.class));
	}
}
