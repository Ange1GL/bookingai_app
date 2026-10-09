package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.BlacklistCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.result.BlacklistResult;

public interface BlacklistCustomerUseCase {
	BlacklistResult blacklist(BlacklistCustomerCommand command);
}
