package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;

public interface HandleCustomerMessageUseCase {
	void handle(HandleCustomerMessageCommand command);
}
