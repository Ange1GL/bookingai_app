package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreateCustomerCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

public interface CreateCustomerUseCase {
	Customer create(CreateCustomerCommand command);
}
