package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.UpdateUserCommand;
import com.github.angellariosacosta.bookingapp.domain.model.User;

public interface UpdateUserUseCase {

	User update(UpdateUserCommand command);
}
