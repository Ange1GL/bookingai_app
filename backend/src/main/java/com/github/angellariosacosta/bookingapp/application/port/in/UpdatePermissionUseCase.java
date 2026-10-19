package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.UpdatePermissionCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

public interface UpdatePermissionUseCase {

	Permission update(UpdatePermissionCommand command);
}
