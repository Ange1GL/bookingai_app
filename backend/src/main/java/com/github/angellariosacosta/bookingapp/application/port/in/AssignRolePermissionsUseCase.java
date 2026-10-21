package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.AssignRolePermissionsCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

public interface AssignRolePermissionsUseCase {

	Role assign(AssignRolePermissionsCommand command);
}
