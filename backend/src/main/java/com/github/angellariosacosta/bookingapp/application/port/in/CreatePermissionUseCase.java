package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreatePermissionCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

public interface CreatePermissionUseCase {

	Permission create(CreatePermissionCommand command);
}
