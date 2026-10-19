package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.UpdateRoleCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

public interface UpdateRoleUseCase {

	Role update(UpdateRoleCommand command);
}
