package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.domain.model.Permission;

public interface GetPermissionUseCase {

	Permission get(Long id);
}
