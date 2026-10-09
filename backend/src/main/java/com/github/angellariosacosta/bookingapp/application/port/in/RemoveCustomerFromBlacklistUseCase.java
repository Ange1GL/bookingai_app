package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.RemoveCustomerFromBlacklistCommand;

public interface RemoveCustomerFromBlacklistUseCase {
	void remove(RemoveCustomerFromBlacklistCommand command);
}
