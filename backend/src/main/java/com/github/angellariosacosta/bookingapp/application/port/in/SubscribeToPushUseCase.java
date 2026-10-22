package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.SubscribeToPushCommand;

public interface SubscribeToPushUseCase {

	void subscribe(SubscribeToPushCommand command);
}
