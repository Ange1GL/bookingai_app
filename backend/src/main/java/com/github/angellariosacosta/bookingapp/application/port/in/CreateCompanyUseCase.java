package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreateCompanyCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

public interface CreateCompanyUseCase {
	Company create(CreateCompanyCommand command);
}
