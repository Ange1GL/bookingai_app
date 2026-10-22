package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.UnsubscribeFromPushCommand;

public interface UnsubscribeFromPushUseCase {

	void unsubscribe(UnsubscribeFromPushCommand command);
}
