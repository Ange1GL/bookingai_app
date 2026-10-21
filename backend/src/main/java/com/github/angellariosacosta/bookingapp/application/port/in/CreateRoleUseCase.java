package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreateRoleCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

public interface CreateRoleUseCase {

	Role create(CreateRoleCommand command);
}
