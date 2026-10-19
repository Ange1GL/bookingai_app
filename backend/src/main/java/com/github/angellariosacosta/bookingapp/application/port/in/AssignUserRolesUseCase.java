package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.AssignUserRolesCommand;
import com.github.angellariosacosta.bookingapp.domain.model.User;

public interface AssignUserRolesUseCase {

	User assign(AssignUserRolesCommand command);
}
