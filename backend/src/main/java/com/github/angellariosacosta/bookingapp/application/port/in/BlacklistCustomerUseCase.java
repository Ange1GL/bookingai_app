package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.BlacklistCustomerCommand;

public interface BlacklistCustomerUseCase {
	void blacklist(BlacklistCustomerCommand command);
}
